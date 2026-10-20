package pe.edu.upeu.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import pe.edu.upeu.data.local.ProductoLocal
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReconciliacionOfflineTest {
    private val cabeceras = headersOf(HttpHeaders.ContentType, "application/json")
    private val confirmado = """{"id":41,"nombre":"Prueba","precio":4.5,"stock":7,"estado":true,"categoriaId":1}"""
    private val pagina = """{"contenido":[$confirmado],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
    private val logger = object : Logger { override fun log(message: String) = Unit }

    @Test
    fun respuestaPostValidaReconciliaIdYNoReenvia() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            var envios = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method == HttpMethod.Post) {
                    envios++
                    respond(confirmado, headers = cabeceras)
                } else respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                repo.sincronizar()
                repo.sincronizar()
                assertEquals(1, envios)
                assertTrue(local.pendientes().isEmpty())
                assertTrue(local.pendientesEnRevision().isEmpty())
                assertEquals(listOf(41L), local.listar().map { it.id })
            } finally { cliente.close() }
        }
    }

    @Test
    fun respuestaPerdidaConservaPendientePeroNoLoReenvia() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            var envios = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method == HttpMethod.Post) {
                    envios++
                    throw java.io.IOException("respuesta perdida tras crear en servidor")
                }
                respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                val error = runCatching { repo.sincronizar() }.exceptionOrNull()
                assertEquals(ErrorApi.SinConexion, assertIs<ErrorApiException>(error).error)
                repo.sincronizar()
                assertEquals(1, envios)
                assertEquals(listOf(-1L), local.pendientesEnRevision().map { it.id })
                assertEquals(setOf(-1L, 41L), local.listar().map { it.id }.toSet())
            } finally { cliente.close() }
        }
    }

    @Test
    fun sinConexionAntesDelPostConservaPendienteYLuegoSincroniza() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Sin red", 4.5, 7, categoriaId = 1))
            var enLinea = false
            var envios = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (!enLinea) throw java.io.IOException("sin conexión")
                if (solicitud.method == HttpMethod.Post) {
                    envios++
                    respond(confirmado, headers = cabeceras)
                } else respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                assertTrue(runCatching { repo.sincronizar() }.isFailure)
                assertEquals(0, envios)
                assertEquals(listOf(-1L), local.pendientes().map { it.id })
                assertTrue(local.pendientesEnRevision().isEmpty())

                enLinea = true
                repo.sincronizar()
                assertEquals(1, envios)
                assertTrue(local.pendientes().isEmpty())
                assertTrue(local.pendientesEnRevision().isEmpty())
                assertEquals(listOf(41L), local.listar().map { it.id })
            } finally { cliente.close() }
        }
    }

    @Test
    fun rechazoDeSesionEnPostDevuelvePendienteALaCola() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method == HttpMethod.Post) {
                    respond("""{"message":"Autenticación requerida"}""", HttpStatusCode.Unauthorized, cabeceras)
                } else respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                val error = runCatching { repo.sincronizar() }.exceptionOrNull()
                assertEquals(ErrorApi.SesionExpirada, assertIs<ErrorApiException>(error).error)
                assertEquals(listOf(-1L), local.pendientes().map { it.id })
                assertTrue(local.pendientesEnRevision().isEmpty())
            } finally { cliente.close() }
        }
    }

    @Test
    fun conflictoDuplicadoNoEliminaNiReenviaPendiente() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            var envios = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method == HttpMethod.Post) {
                    envios++
                    respond("""{"message":"Nombre duplicado"}""", HttpStatusCode.Conflict, cabeceras)
                } else respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                val error = runCatching { repo.sincronizar() }.exceptionOrNull()
                assertIs<ErrorApi.Conflicto>(assertIs<ErrorApiException>(error).error)
                repo.sincronizar()
                assertEquals(1, envios)
                assertEquals(listOf(-1L), local.pendientesEnRevision().map { it.id })
            } finally { cliente.close() }
        }
    }

    @Test
    fun sincronizacionesConcurrentesHacenUnSoloPost() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            val iniciado = CompletableDeferred<Unit>()
            val continuar = CompletableDeferred<Unit>()
            var envios = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method == HttpMethod.Post) {
                    envios++
                    iniciado.complete(Unit)
                    continuar.await()
                    respond(confirmado, headers = cabeceras)
                } else respond(pagina, headers = cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                val primero = async { repo.sincronizar() }
                withTimeout(3_000) { iniciado.await() }
                val segundo = async { repo.sincronizar() }
                continuar.complete(Unit)
                withTimeout(3_000) { primero.await(); segundo.await() }
                assertEquals(1, envios)
                assertTrue(local.pendientesEnRevision().isEmpty())
            } finally { cliente.close() }
        }
    }
}
