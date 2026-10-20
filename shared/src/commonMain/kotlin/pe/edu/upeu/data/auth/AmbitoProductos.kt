package pe.edu.upeu.data.auth

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.data.local.CreadorDeDriver
import pe.edu.upeu.data.local.ProductoLocal
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.CreadorMotorHttp
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.data.repository.ProductoRepositorioNoDisponible
import pe.edu.upeu.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.domain.auth.CoordinadorSesion
import pe.edu.upeu.domain.auth.Sesion
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Posee juntos identidad, token fijo, cliente, conexión y trabajos de una sesión. */
class CoordinadorAmbitoProductos(
    private val vinculacion: VinculacionProtegida,
    private val creadorDriver: CreadorDeDriver,
    private val creadorMotor: CreadorMotorHttp,
    private val urlBase: String,
    private val logger: Logger,
    private val esDepuracion: Boolean,
    private val almacen: AlmacenSeguro,
    private val coordinadorSesion: CoordinadorSesion
) {
    private val mutex = Mutex()
    private val cierres = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val actual = MutableStateFlow<AmbitoProductos?>(null)
    private var proximaGeneracion = 0L

    /** Solamente AuthRepositorioRest llama esto después de validar login o /auth/me. */
    suspend fun activar(sesion: Sesion): Boolean {
        val archivo = vinculacion.verificarOVincular(claveCuentaValidada(urlBase, sesion.usuario))
            ?: run {
                revocar()
                return false
            }
        var anterior: AmbitoProductos? = null
        mutex.withLock {
            val vigente = actual.value
            if (vigente != null && vigente.token == sesion.token && vigente.usuario == sesion.usuario) return true
            val generacion = ++proximaGeneracion
            val driver = creadorDriver.crear(archivo)
            val cliente = try {
                crearHttpClient(
                    creadorMotor.crear(), urlBase, logger, esDepuracion = esDepuracion,
                    tokenFijo = sesion.token,
                    al401DeSesion = { cierres.launch { invalidarSiVigente(generacion) } }
                )
            } catch (error: Throwable) {
                driver.close()
                throw error
            }
            val nuevo = try {
                AmbitoProductos(generacion, sesion.usuario, sesion.token, driver, cliente)
            } catch (error: Throwable) {
                cliente.close()
                driver.close()
                throw error
            }
            anterior = vigente
            vigente?.desactivar()
            actual.value = nuevo
        }
        anterior?.cerrarCuandoLibre()
        return true
    }

    fun capturarRepositorio(): ProductoRepository = actual.value?.let(::RepositorioDeAmbito)
        ?: ProductoRepositorioNoDisponible()

    suspend fun revocar() {
        val anterior = mutex.withLock {
            actual.value.also { actual.value = null; it?.desactivar() }
        }
        anterior?.cerrarCuandoLibre()
    }

    suspend fun revocarSiToken(token: String) {
        val anterior = mutex.withLock {
            actual.value?.takeIf { it.token == token }?.also {
                actual.value = null
                it.desactivar()
            }
        }
        anterior?.cerrarCuandoLibre()
    }

    fun programarRevocacionToken(token: String) {
        cierres.launch { revocarSiToken(token) }
    }

    private suspend fun invalidarSiVigente(generacion: Long) {
        val anterior = mutex.withLock {
            val vigente = actual.value?.takeIf { it.generacion == generacion } ?: return@withLock null
            actual.value = null
            vigente.desactivar()
            if (almacen.leerToken() == vigente.token) {
                almacen.borrarToken()
                coordinadorSesion.expirada()
            }
            vigente
        }
        anterior?.cerrarCuandoLibre()
    }
}

private class AmbitoProductos(
    val generacion: Long,
    val usuario: String,
    val token: String,
    private val driver: SqlDriver,
    private val cliente: HttpClient
) {
    private val vigente = MutableStateFlow(true)
    private val mutex = Mutex()
    private val trabajos = mutableSetOf<Job>()
    private val repositorio = ProductoRepositorioOffline(
        ProductoLocal(PharmaDb(driver)), ProductoApi(cliente), CategoriaApi(cliente), ::comprobarVigencia
    )

    fun comprobarVigencia() {
        check(vigente.value) { "El ámbito de productos ya no está vigente" }
    }

    suspend fun <T> ejecutar(bloque: suspend ProductoRepositorioOffline.() -> T): T {
        val trabajo = requireNotNull(currentCoroutineContext()[Job])
        mutex.withLock { comprobarVigencia(); trabajos += trabajo }
        try {
            return repositorio.bloque().also { comprobarVigencia() }
        } finally {
            mutex.withLock { trabajos -= trabajo }
        }
    }

    fun desactivar() {
        vigente.value = false
        // El cierre espera a los trabajos registrados antes de cerrar SQLite.
    }

    suspend fun cerrarCuandoLibre() {
        val activos = mutex.withLock { trabajos.toList() }
        activos.forEach { it.cancel() }
        activos.joinAll()
        cliente.close()
        driver.close()
    }
}

/** Captura un ámbito inmutable: un ViewModel viejo nunca se redirige a la cuenta nueva. */
private class RepositorioDeAmbito(private val ambito: AmbitoProductos) : ProductoRepository {
    override fun observar(): Flow<List<Producto>> = flow {
        ambito.ejecutar { emitAll(observar()) }
    }
    override suspend fun sincronizar() = ambito.ejecutar { sincronizar() }
    override suspend fun registrar(producto: Producto) = ambito.ejecutar { registrar(producto) }
    override suspend fun listar() = ambito.ejecutar { listar() }
    override suspend fun obtener(id: Long) = ambito.ejecutar { obtener(id) }
    override suspend fun actualizar(producto: Producto) = ambito.ejecutar { actualizar(producto) }
    override suspend fun eliminar(id: Long) = ambito.ejecutar { eliminar(id) }
    override suspend fun listarCategorias(): List<Categoria> = ambito.ejecutar { listarCategorias() }
}
