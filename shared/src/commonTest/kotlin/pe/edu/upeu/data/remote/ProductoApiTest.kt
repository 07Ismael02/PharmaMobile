package pe.edu.upeu.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductoApiTest {
    private val silentLogger = object : Logger {
        override fun log(message: String) = Unit
    }
    private val productoJson = """{"id":7,"nombre":"Prueba S08","precio":4.5,"stock":8,"estado":true,"categoriaId":3,"categoriaNombre":"Pruebas"}"""
    private val request = ProductoRequestDto("Prueba S08", 4.5, 8, true, 3)
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun listarObtieneContenidoPaginado() = runTest {
        val client = crearHttpClient(MockEngine { llamada ->
            assertEquals(HttpMethod.Get, llamada.method)
            assertEquals("/api/v1/productos", llamada.url.encodedPath)
            respond("""{"contenido":[$productoJson],"pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true}""", headers = jsonHeaders)
        }, "http://localhost/api/v1/", silentLogger)
        try {
            assertEquals(7L, ProductoApi(client).listar().contenido.single().id)
        } finally {
            client.close()
        }
    }

    @Test
    fun obtenerCrearActualizarYEliminarRespetanVerbosY204SinCuerpo() = runTest {
        val verbos = mutableListOf<HttpMethod>()
        val client = crearHttpClient(MockEngine { llamada ->
            verbos += llamada.method
            if (llamada.method == HttpMethod.Delete) {
                respond("", HttpStatusCode.NoContent)
            } else {
                respond(productoJson, if (llamada.method == HttpMethod.Post) HttpStatusCode.Created else HttpStatusCode.OK, jsonHeaders)
            }
        }, "http://localhost/api/v1/", silentLogger)
        try {
            val api = ProductoApi(client)
            assertEquals(7L, api.obtener(7).id)
            assertEquals(7L, api.crear(request).id)
            assertEquals(7L, api.actualizar(7, request).id)
            api.eliminar(7)
            assertEquals(listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete), verbos)
        } finally {
            client.close()
        }
    }

    @Test
    fun traduce400ConErroresDeCampoY404Y409() = runTest {
        val casos = listOf(
            HttpStatusCode.BadRequest to """{"status":400,"error":"Bad Request","message":"Validación","path":"/api/v1/productos","validationErrors":{"nombre":"Mínimo 3 caracteres"}}""",
            HttpStatusCode.NotFound to """{"status":404,"error":"Not Found","message":"No existe","path":"/api/v1/productos/7"}""",
            HttpStatusCode.Conflict to """{"status":409,"error":"Conflict","message":"Nombre duplicado","path":"/api/v1/productos"}"""
        )
        for ((codigo, cuerpo) in casos) {
            val client = crearHttpClient(MockEngine { respond(cuerpo, codigo, jsonHeaders) }, "http://localhost/api/v1/", silentLogger)
            try {
                val fallo = ejecutarLlamada { ProductoApi(client).obtener(7) }.exceptionOrNull()
                val error = assertIs<ErrorApiException>(fallo).error
                when (codigo) {
                    HttpStatusCode.BadRequest -> assertEquals("Mínimo 3 caracteres", assertIs<ErrorApi.Validacion>(error).porCampo["nombre"])
                    HttpStatusCode.NotFound -> assertIs<ErrorApi.NoEncontrado>(error)
                    else -> assertEquals("Nombre duplicado", assertIs<ErrorApi.Conflicto>(error).mensaje)
                }
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun traduce500YPropagaCancelacion() = runTest {
        val client = crearHttpClient(MockEngine { respond("fallo", HttpStatusCode.InternalServerError) }, "http://localhost/api/v1/", silentLogger)
        try {
            val fallo = ejecutarLlamada { ProductoApi(client).listar() }.exceptionOrNull()
            assertIs<ErrorApi.Servidor>(assertIs<ErrorApiException>(fallo).error)
        } finally {
            client.close()
        }
        var propagada = false
        try {
            ejecutarLlamada<Unit> { throw CancellationException("cancelada") }
        } catch (_: CancellationException) {
            propagada = true
        }
        assertTrue(propagada)
    }
}
