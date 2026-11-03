package pe.edu.upeu.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.data.local.ProductoLocal
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Integrador13SincronizacionTest {
    private val cabeceras = headersOf(HttpHeaders.ContentType, "application/json")
    private val logger = object : Logger { override fun log(message: String) = Unit }

    @Test
    fun cincoFallosDePreflightNoEnvianPostYQuedanVisibles() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            val creado = local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            var post = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.url.encodedPath.endsWith("/productos") && solicitud.method.value == "POST") post++
                respond("{}", HttpStatusCode.ServiceUnavailable, cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                repeat(5) { assertTrue(runCatching { repo.sincronizar() }.isFailure) }
                assertEquals(0, post)
                assertEquals(5, local.intentos(creado.id))
                assertEquals(0, repo.observarPendientes().first())
                assertEquals(1, repo.observarFallidas().first())
                assertEquals(listOf(creado.id), local.listar().map { it.id })
            } finally { cliente.close() }
        }
    }

    @Test
    fun rechazoPermanenteDePreflightNoReintentaNiBorra() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val local = ProductoLocal(PharmaDb(driver))
            val creado = local.guardarPendiente(Producto(0, "Prueba", 4.5, 7, categoriaId = 1))
            var solicitudes = 0
            val cliente = crearHttpClient(MockEngine { solicitud ->
                if (solicitud.method.value == "POST") solicitudes++
                respond("{}", HttpStatusCode.Forbidden, cabeceras)
            }, "https://ejemplo.local/api/v1/", logger)
            try {
                val repo = ProductoRepositorioOffline(local, ProductoApi(cliente), CategoriaApi(cliente))
                assertTrue(runCatching { repo.sincronizar() }.isFailure)
                assertEquals(5, local.intentos(creado.id))
                assertEquals(1, repo.observarFallidas().first())
                assertTrue(runCatching { repo.sincronizar() }.isFailure)
                assertEquals(0, solicitudes)
                assertEquals(listOf(creado.id), local.listar().map { it.id })
            } finally { cliente.close() }
        }
    }
}
