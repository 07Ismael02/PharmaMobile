package pe.edu.upeu.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EstadoSyncLocalTest {
    @Test
    fun fechaPersisteEnBaseDeCuentaYSoloCambiaTrasSincronizar() = runBlocking {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            PharmaDb.Schema.create(driver)
            val base = PharmaDb(driver)
            val estado = EstadoSyncLocal(base)
            assertNull(estado.ultimaSincronizacion())
            var responder = true
            val cliente = crearHttpClient(MockEngine {
                if (!responder) throw java.io.IOException("sin red")
                respond("""{"contenido":[],"pagina":0,"tamanio":100,"totalElementos":0,"totalPaginas":0,"ultima":true}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }, "https://ejemplo.local/api/v1/", object : Logger { override fun log(message: String) = Unit })
            try {
                val repositorio = ProductoRepositorioOffline(
                    ProductoLocal(base), ProductoApi(cliente), CategoriaApi(cliente),
                    estadoSync = estado
                )
                repositorio.sincronizar()
                val primera = assertNotNull(estado.ultimaSincronizacion())
                assertEquals(primera, estado.observarUltimaSincronizacion().first())
                assertEquals(primera, EstadoSyncLocal(PharmaDb(driver)).ultimaSincronizacion())
                responder = false
                assertTrue(runCatching { repositorio.sincronizar() }.isFailure)
                assertEquals(primera, estado.ultimaSincronizacion())
            } finally { cliente.close() }
        }
    }
}
