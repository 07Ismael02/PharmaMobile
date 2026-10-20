package pe.edu.upeu.data.auth

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import pe.edu.upeu.data.local.CreadorDeDriver
import pe.edu.upeu.data.remote.CreadorMotorHttp
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.domain.auth.CoordinadorSesion
import pe.edu.upeu.domain.auth.Sesion
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AmbitoProductosTest {
    private val url = "https://ejemplo.local/api/v1/"
    private val logger = object : Logger { override fun log(message: String) = Unit }

    private class AlmacenMemoria(var token: String? = null) : AlmacenSeguro {
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken(): String? = token
        override fun borrarToken() { token = null }
    }

    private class VinculacionMemoria : VinculacionProtegida {
        private var clave: String? = null
        override fun verificarOVincular(claveCuenta: String): String? {
            if (clave == null) clave = claveCuenta
            return "pharma-cuenta-0123456789abcdef0123456789abcdef.db".takeIf { clave == claveCuenta }
        }
    }

    private class DriversMemoria : CreadorDeDriver {
        val nombres = mutableListOf<String>()
        val abiertos = mutableListOf<SqlDriver>()
        override fun crear(nombreArchivo: String): SqlDriver {
            nombres += nombreArchivo
            return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
                PharmaDb.Schema.create(it)
                abiertos += it
            }
        }
    }

    private fun gestor(
        vinculacion: VinculacionMemoria,
        drivers: DriversMemoria,
        motor: CreadorMotorHttp,
        almacen: AlmacenMemoria = AlmacenMemoria(),
        coordinador: CoordinadorSesion = CoordinadorSesion()
    ) = CoordinadorAmbitoProductos(vinculacion, drivers, motor, url, logger, false, almacen, coordinador)

    private fun sesion(usuario: String, token: String) = Sesion(token, usuario, "Prueba", setOf("CATALOGO_LEER"))

    @Test
    fun otraCuentaNoAbreBaseNiAccedeAPendientes() = runBlocking {
        val drivers = DriversMemoria()
        val gestor = gestor(VinculacionMemoria(), drivers,
            object : CreadorMotorHttp {
                override fun crear(): HttpClientEngine = MockEngine { throw java.io.IOException("sin red") }
            })
        assertTrue(gestor.activar(sesion("Admin", "token-A")))
        val pendiente = gestor.capturarRepositorio().registrar(
            Producto(0, "Pendiente A", 4.5, 7, true, 1)
        )
        assertTrue(pendiente.id < 0)
        assertEquals(1, PharmaDb(drivers.abiertos.single()).productoQueries.listarPendientes().executeAsList().size)
        assertFalse(gestor.activar(sesion("vendedor", "token-B")))
        assertEquals(1, drivers.nombres.size)
        assertFailsWith<Exception> { gestor.capturarRepositorio().listar() }
        assertTrue(drivers.nombres.single().startsWith("pharma-cuenta-"))
        assertFalse(drivers.nombres.single().contains("admin"))
        assertFalse(drivers.nombres.single() == "pharma.db")
        gestor.revocar()
    }

    @Test
    fun logoutRevocaTrabajoPendienteYNoLoRedirigeAOtraCuenta() = runBlocking {
        val iniciado = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        val drivers = DriversMemoria()
        val almacen = AlmacenMemoria("token-A")
        val coordinador = CoordinadorSesion().apply { activa(sesion("admin", "token-A")) }
        val gestor = gestor(VinculacionMemoria(), drivers,
            object : CreadorMotorHttp {
                override fun crear(): HttpClientEngine = MockEngine {
                    iniciado.complete(Unit)
                    continuar.await()
                    respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }, almacen, coordinador)
        assertTrue(gestor.activar(sesion("admin", "token-A")))
        val repositorioA = gestor.capturarRepositorio()
        val solicitud = async { runCatching { repositorioA.listar() } }
        withTimeout(3_000) { iniciado.await() }
        val clienteAuth = crearHttpClient(MockEngine { respond("{}") }, url, logger, almacen, coordinador)
        try {
            withTimeout(3_000) {
                AuthRepositorioRest(clienteAuth, almacen, coordinador, gestor).cerrarSesion()
            }
        } finally { clienteAuth.close() }
        assertTrue(solicitud.isCancelled || solicitud.await().isFailure)
        assertNull(almacen.leerToken())
        assertNull(coordinador.estado.value.sesion)
        assertFailsWith<Exception> { gestor.capturarRepositorio().listar() }
        assertFalse(gestor.activar(sesion("vendedor", "token-B")))
        assertEquals(1, drivers.nombres.size)
    }

    @Test
    fun tokenFijoNoCambiaAunqueCambieElAlmacenGlobal() = runBlocking {
        val cabeceras = mutableListOf<String?>()
        val cliente = crearHttpClient(MockEngine { solicitud ->
            cabeceras += solicitud.headers[HttpHeaders.Authorization]
            respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }, url, logger, tokenFijo = "token-A")
        try {
            cliente.get("productos")
            cliente.get("productos")
            assertEquals(listOf<String?>("Bearer token-A", "Bearer token-A"), cabeceras)
        } finally { cliente.close() }
    }

    @Test
    fun respuesta401AntiguaNoBorraSesionNueva() = runBlocking {
        val recibido = CompletableDeferred<Unit>()
        val continuar = CompletableDeferred<Unit>()
        val almacen = AlmacenMemoria("token-A")
        val coordinador = CoordinadorSesion().apply { activa(sesion("admin", "token-A")) }
        val cliente = crearHttpClient(MockEngine {
            recibido.complete(Unit)
            continuar.await()
            respond("{}", HttpStatusCode.Unauthorized)
        }, url, logger, almacen, coordinador)
        try {
            val solicitudA = async { runCatching { cliente.get("productos") } }
            withTimeout(3_000) { recibido.await() }
            almacen.guardarToken("token-B")
            coordinador.activa(sesion("admin", "token-B"))
            continuar.complete(Unit)
            assertTrue(solicitudA.await().isFailure)
            assertEquals("token-B", almacen.leerToken())
            assertEquals("token-B", coordinador.estado.value.sesion?.token)
        } finally { cliente.close() }
    }

    @Test
    fun respuestaTardiaDeTokenAnteriorNoRevocaNuevoAmbito() = runBlocking {
        val almacen = AlmacenMemoria("token-A")
        val coordinador = CoordinadorSesion().apply { activa(sesion("admin", "token-A")) }
        val cabeceras = mutableListOf<String?>()
        val gestor = gestor(VinculacionMemoria(), DriversMemoria(),
            object : CreadorMotorHttp {
                override fun crear(): HttpClientEngine = MockEngine { solicitud ->
                    cabeceras += solicitud.headers[HttpHeaders.Authorization]
                    respond("""{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }, almacen, coordinador)
        assertTrue(gestor.activar(sesion("admin", "token-A")))
        val repositorioAnterior = gestor.capturarRepositorio()
        almacen.guardarToken("token-B")
        coordinador.activa(sesion("admin", "token-B"))
        assertTrue(gestor.activar(sesion("admin", "token-B")))
        gestor.revocarSiToken("token-A")
        assertFailsWith<Exception> { repositorioAnterior.listar() }
        assertTrue(gestor.capturarRepositorio().listar().isEmpty())
        assertEquals(listOf<String?>("Bearer token-B"), cabeceras)
        assertEquals("token-B", almacen.leerToken())
        assertEquals("token-B", coordinador.estado.value.sesion?.token)
        gestor.revocar()
    }

    @Test
    fun claveIncluyeOrigenYNormalizaUsername() {
        assertEquals(claveCuentaValidada(url, " ADMIN "), claveCuentaValidada(url, "admin"))
        assertFalse(claveCuentaValidada(url, "admin") ==
            claveCuentaValidada("https://otro.local/api/v1/", "admin"))
        assertNull(VinculacionMemoria().apply { verificarOVincular(claveCuentaValidada(url, "admin")) }
            .verificarOVincular(claveCuentaValidada(url, "vendedor")))
    }
}
