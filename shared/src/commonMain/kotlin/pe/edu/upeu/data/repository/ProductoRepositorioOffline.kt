package pe.edu.upeu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.delay
import pe.edu.upeu.data.local.ProductoLocal
import pe.edu.upeu.data.local.CategoriaLocal
import pe.edu.upeu.data.local.EstadoSyncLocal
import pe.edu.upeu.data.mapper.toDomain
import pe.edu.upeu.data.mapper.toRequest
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.data.remote.ejecutarLlamada
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi,
    private val categorias: CategoriaApi,
    private val comprobarAmbito: () -> Unit = {},
    private val categoriasLocales: CategoriaLocal? = null,
    private val estadoSync: EstadoSyncLocal? = null
) : ProductoRepository {
    private val mutexSincronizacion = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()
    override fun observarPendientes(): Flow<Int> = local.observarPendientes()
    override fun observarFallidas(): Flow<Int> = local.observarFallidas()
    override fun observarUltimaSincronizacion(): Flow<Long?> =
        estadoSync?.observarUltimaSincronizacion() ?: super.observarUltimaSincronizacion()

    /** Compatibilidad para consultas puntuales; la interfaz observa el Flow local. */
    override suspend fun listar(): List<Producto> {
        try {
            sincronizar()
        } catch (error: ErrorApiException) {
            if (error.error != ErrorApi.SinConexion && error.error != ErrorApi.TiempoAgotado &&
                error.error != ErrorApi.Servidor) throw error
        }
        return local.listar()
    }

    override suspend fun registrar(producto: Producto): Producto {
        comprobarAmbito()
        producto.toRequest() // Valida la categoría antes de crear un pendiente imposible de subir.
        val guardado = local.guardarPendiente(producto)
        try {
            sincronizar()
        } catch (error: ErrorApiException) {
            if (!error.error.esTransitorio()) throw error
        }
        return guardado
    }

    override suspend fun sincronizar() {
        comprobarAmbito()
        mutexSincronizacion.lock()
        try {
            comprobarAmbito()
            subirPendientes()
            val productos = descargarCatalogo()
            comprobarAmbito()
            local.reemplazarSincronizados(productos)
            comprobarAmbito()
            estadoSync?.registrarExito()
        } finally {
            mutexSincronizacion.unlock()
        }
    }

    private suspend fun subirPendientes() {
        val pendientes = local.pendientes()
        pendientes.forEach { pendiente ->
            comprobarAmbito()
            // GET es seguro para reintentar. El POST nunca se reintenta tras una respuesta perdida.
            val previos = local.intentos(pendiente.id)
            if (previos > 0) delay(1_000L shl (previos - 1).coerceAtMost(4))
            comprobarAmbito()
            try {
                ejecutarLlamada { remoto.listar(0, 1) }.getOrThrow()
            } catch (error: ErrorApiException) {
                val motivo = error.error.motivoSinDatosSensibles()
                if (error.error.esTransitorio()) local.sumarIntento(pendiente.id, motivo)
                else local.marcarFallida(pendiente.id, motivo)
                throw error
            }
            local.marcarIntentoEnvio(pendiente.id)
            comprobarAmbito()
            val confirmado = try {
                ejecutarLlamada { remoto.crear(pendiente.toRequest()).toDomain() }.getOrThrow()
            } catch (error: ErrorApiException) {
                // 401/403 los emite Spring Security antes del controlador: no hubo creación remota.
                // Cualquier otro fallo (red, timeout, 409, 5xx) queda en revisión sin reenvío.
                if (error.error == ErrorApi.SesionExpirada || error.error == ErrorApi.SinPermiso) {
                    local.revertirIntentoEnvio(pendiente.id)
                } else {
                    local.anotarErrorEnvio(pendiente.id, error.error.motivoSinDatosSensibles())
                }
                throw error
            }
            comprobarAmbito()
            local.confirmarPendiente(pendiente.id, confirmado)
        }
    }

    private suspend fun descargarCatalogo(): List<Producto> {
        val primera = ejecutarLlamada { remoto.listar(0, TAMANIO_PAGINA) }.getOrThrow()
        val totalPaginas = primera.totalPaginas
        check(totalPaginas in 0..MAX_PAGINAS) { "El catálogo supera el límite seguro de páginas" }
        check(primera.totalElementos >= 0) { "Total de productos inválido" }
        val productos = mutableListOf<Producto>()

        fun agregar(pagina: PaginaResponseDto<ProductoResponseDto>, indice: Int) {
            check(pagina.pagina == indice && pagina.totalPaginas == totalPaginas &&
                pagina.totalElementos == primera.totalElementos &&
                pagina.contenido.size <= TAMANIO_PAGINA) {
                "La paginación cambió durante la sincronización"
            }
            productos += pagina.contenido.map { it.toDomain() }
        }

        agregar(primera, 0)
        for (indice in 1 until totalPaginas) {
            agregar(ejecutarLlamada { remoto.listar(indice, TAMANIO_PAGINA) }.getOrThrow(), indice)
        }
        check(productos.size.toLong() == primera.totalElementos &&
            productos.map { it.id }.distinct().size == productos.size) {
            "El catálogo paginado está incompleto o duplicado"
        }
        return productos
    }

    override suspend fun obtener(id: Long): Producto {
        if (id < 0) return local.buscar(id)
            ?: throw ErrorApiException(ErrorApi.NoEncontrado)
        val producto = ejecutarLlamada { remoto.obtener(id).toDomain() }.getOrThrow()
        comprobarAmbito()
        local.guardarSincronizado(producto)
        return producto
    }

    override suspend fun actualizar(producto: Producto): Producto {
        requireIdServidor(producto.id)
        val actualizado = ejecutarLlamada {
            remoto.actualizar(producto.id, producto.toRequest()).toDomain()
        }.getOrThrow()
        comprobarAmbito()
        local.guardarSincronizado(actualizado)
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        requireIdServidor(id)
        ejecutarLlamada { remoto.eliminar(id) }.getOrThrow()
        comprobarAmbito()
        local.marcarInactivo(id)
    }

    override suspend fun listarCategorias(): List<Categoria> {
        comprobarAmbito()
        return try {
            val recibidas = ejecutarLlamada { categorias.listar().map { it.toDomain() } }.getOrThrow()
            comprobarAmbito()
            categoriasLocales?.reemplazarSincronizadas(recibidas)
            recibidas
        } catch (error: ErrorApiException) {
            comprobarAmbito()
            if (error.error != ErrorApi.SinConexion && error.error != ErrorApi.TiempoAgotado) throw error
            categoriasLocales?.listar()?.takeIf { it.isNotEmpty() } ?: throw error
        }
    }

    private fun requireIdServidor(id: Long) {
        if (id <= 0) throw ErrorApiException(
            ErrorApi.Conflicto("Sincronice el producto pendiente antes de editarlo o darlo de baja")
        )
    }

    private companion object {
        const val TAMANIO_PAGINA = 100
        const val MAX_PAGINAS = 1_000
    }
}

private fun ErrorApi.esTransitorio(): Boolean =
    this == ErrorApi.SinConexion || this == ErrorApi.TiempoAgotado || this == ErrorApi.Servidor

private fun ErrorApi.motivoSinDatosSensibles(): String = when (this) {
    ErrorApi.SinConexion -> "Sin conexión"
    ErrorApi.TiempoAgotado -> "Tiempo agotado"
    ErrorApi.Servidor -> "Servidor no disponible"
    ErrorApi.SinPermiso -> "Sin privilegio"
    ErrorApi.SesionExpirada -> "Sesión expirada"
    ErrorApi.NoEncontrado -> "Recurso no encontrado"
    is ErrorApi.Conflicto -> "Conflicto; revisar antes de reenviar"
    is ErrorApi.Validacion -> "Validación rechazada"
}
