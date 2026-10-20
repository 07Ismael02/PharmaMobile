package pe.edu.upeu.data.local

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Persistencia SQLDelight y sincronización offline-first en código común.
 * Usa SQLite en memoria y un servidor simulado (MockEngine): no accede a Oracle.
 */
class PersistenciaSincronizacionCommonTest {
    private val cabeceras = headersOf(HttpHeaders.ContentType, "application/json")
    private val confirmado = """{"id":41,"nombre":"Prueba","precio":4.5,"stock":7,"estado":true,"categoriaId":1}"""
    private val pagina = """{"contenido":[$confirmado],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
    private val logger = object : Logger { override fun log(message: String) = Unit }
    private val pendiente = Producto(0, "Prueba", 4.5, 7, categoriaId = 1)

    private fun conBase(prueba: suspend (ProductoLocal) -> Unit) = runTest {
        val driver = crearDriverPrueba()
        try {
            prueba(ProductoLocal(PharmaDb(driver)))
        } finally {
            driver.close()
        }
    }

    private suspend fun conRepositorio(
        local: ProductoLocal,
        servidor: MockRequestHandler,
        prueba: suspend (ProductoRepositorioOffline) -> Unit
    ) {
        val cliente = crearHttpClient(MockEngine(servidor), "https://ejemplo.local/api/v1/", logger)
        try {
            prueba(ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente)))
        } finally {
            cliente.close()
        }
    }

    @Test
    fun registroOfflineAsignaIdsNegativosYQuedaPorEnviar() = conBase { local ->
        val primero = local.guardarPendiente(pendiente)
        val segundo = local.guardarPendiente(pendiente.copy(nombre = "Segundo"))
        assertEquals(-1L, primero.id)
        assertEquals(-2L, segundo.id)
        assertEquals(setOf(-1L, -2L), local.pendientes().map { it.id }.toSet())
        assertTrue(local.pendientesEnRevision().isEmpty())
    }

    @Test
    fun observarEmiteProductosPersistidosEnSqlDelight() = conBase { local ->
        local.guardarSincronizado(Producto(43, "Sincronizado", 3.0, 2, categoriaId = 1))
        local.guardarPendiente(pendiente)
        assertEquals(setOf(-1L, 43L), local.observar().first().map { it.id }.toSet())
    }

    @Test
    fun reemplazarSincronizadosConservaPendientesLocales() = conBase { local ->
        local.guardarSincronizado(Producto(10, "Antiguo", 1.0, 1, categoriaId = 1))
        local.guardarPendiente(pendiente)
        local.reemplazarSincronizados(listOf(Producto(41, "Nuevo", 2.0, 3, categoriaId = 1)))
        assertEquals(setOf(-1L, 41L), local.listar().map { it.id }.toSet())
        assertEquals(listOf(-1L), local.pendientes().map { it.id })
    }

    @Test
    fun intentoDeEnvioMarcadoPasaARevisionYNoSeMarcaDosVeces() = conBase { local ->
        local.guardarPendiente(pendiente)
        local.marcarIntentoEnvio(-1)
        assertTrue(local.pendientes().isEmpty())
        assertEquals(listOf(-1L), local.pendientesEnRevision().map { it.id })
        assertFailsWith<IllegalStateException> { local.marcarIntentoEnvio(-1) }
    }

    @Test
    fun postValidoReconciliaIdLocalConIdServidor() = conBase { local ->
        local.guardarPendiente(pendiente)
        var envios = 0
        conRepositorio(local, { solicitud ->
            if (solicitud.method == HttpMethod.Post) {
                envios++
                respond(confirmado, headers = cabeceras)
            } else respond(pagina, headers = cabeceras)
        }) { repo ->
            repo.sincronizar()
            repo.sincronizar()
        }
        assertEquals(1, envios)
        assertEquals(listOf(41L), local.listar().map { it.id })
        assertTrue(local.pendientes().isEmpty())
        assertTrue(local.pendientesEnRevision().isEmpty())
    }

    @Test
    fun respuestaPostPerdidaQuedaEnRevisionSinReenvio() = conBase { local ->
        local.guardarPendiente(pendiente)
        var envios = 0
        conRepositorio(local, { solicitud ->
            if (solicitud.method == HttpMethod.Post) {
                envios++
                throw IOException("respuesta perdida")
            }
            respond(pagina, headers = cabeceras)
        }) { repo ->
            val error = runCatching { repo.sincronizar() }.exceptionOrNull()
            assertEquals(ErrorApi.SinConexion, assertIs<ErrorApiException>(error).error)
            repo.sincronizar()
        }
        assertEquals(1, envios)
        assertEquals(listOf(-1L), local.pendientesEnRevision().map { it.id })
    }

    @Test
    fun sinConexionConservaPendienteYSincronizaAlVolverLaRed() = conBase { local ->
        local.guardarPendiente(pendiente)
        var enLinea = false
        var envios = 0
        conRepositorio(local, { solicitud ->
            if (!enLinea) throw IOException("sin conexión")
            if (solicitud.method == HttpMethod.Post) {
                envios++
                respond(confirmado, headers = cabeceras)
            } else respond(pagina, headers = cabeceras)
        }) { repo ->
            assertTrue(runCatching { repo.sincronizar() }.isFailure)
            assertEquals(0, envios)
            assertEquals(listOf(-1L), local.pendientes().map { it.id })
            enLinea = true
            repo.sincronizar()
        }
        assertEquals(1, envios)
        assertEquals(listOf(41L), local.listar().map { it.id })
    }

    @Test
    fun conflictoDuplicadoNoEliminaNiReenviaPendiente() = conBase { local ->
        local.guardarPendiente(pendiente)
        var envios = 0
        conRepositorio(local, { solicitud ->
            if (solicitud.method == HttpMethod.Post) {
                envios++
                respond("""{"message":"Nombre duplicado"}""", HttpStatusCode.Conflict, cabeceras)
            } else respond(pagina, headers = cabeceras)
        }) { repo ->
            val error = runCatching { repo.sincronizar() }.exceptionOrNull()
            assertIs<ErrorApi.Conflicto>(assertIs<ErrorApiException>(error).error)
            repo.sincronizar()
        }
        assertEquals(1, envios)
        assertEquals(listOf(-1L), local.pendientesEnRevision().map { it.id })
    }
}
