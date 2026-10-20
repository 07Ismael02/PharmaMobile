package pe.edu.upeu.data.auth

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.plugins.logging.Logger
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pe.edu.upeu.domain.auth.ErrorSesion
import pe.edu.upeu.domain.auth.ErrorSesionException
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.domain.auth.CoordinadorSesion
import pe.edu.upeu.domain.auth.Sesion

class SesionHttpTest {
    private class AlmacenMemoria(var token: String? = null) : AlmacenSeguro {
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken(): String? = token
        override fun borrarToken() { token = null }
    }

    private val loggerSilencioso = object : Logger { override fun log(message: String) = Unit }

    @Test
    fun respuesta401BorraAlmacenCacheYEstado() = runBlocking {
        val almacen = AlmacenMemoria("token-ficticio")
        val coordinador = CoordinadorSesion().apply {
            activa(Sesion("token-ficticio", "almacen", "Prueba", setOf("CATALOGO_LEER")))
        }
        val cabeceras = mutableListOf<String?>()
        val cliente = crearHttpClient(MockEngine { solicitud ->
            cabeceras += solicitud.headers[HttpHeaders.Authorization]
            respond("{}", if (cabeceras.size == 1) HttpStatusCode.Unauthorized else HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://ejemplo.local/api/v1/", loggerSilencioso, almacen, coordinador)

        try {
            assertFailsWith<ClientRequestException> { cliente.get("productos") }
            assertNull(almacen.leerToken())
            assertNull(coordinador.estado.value.sesion)
            cliente.get("productos")
            assertEquals("Bearer token-ficticio", cabeceras.first())
            assertNull(cabeceras.last())
        } finally { cliente.close() }
    }

    @Test
    fun respuesta403ConservaLaSesion() = runBlocking {
        val almacen = AlmacenMemoria("token-ficticio")
        val coordinador = CoordinadorSesion().apply {
            activa(Sesion("token-ficticio", "vendedor", "Prueba", setOf("CATALOGO_LEER")))
        }
        val cliente = crearHttpClient(MockEngine {
            respond("{}", HttpStatusCode.Forbidden, headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://ejemplo.local/api/v1/", loggerSilencioso, almacen, coordinador)

        try {
            assertFailsWith<ClientRequestException> { cliente.get("productos") }
            assertEquals("token-ficticio", almacen.leerToken())
            assertTrue(coordinador.estado.value.sesion != null)
        } finally { cliente.close() }
    }

    @Test
    fun logoutBorraCacheAntesDeLaSiguientePeticion() = runBlocking {
        val almacen = AlmacenMemoria("token-ficticio")
        val coordinador = CoordinadorSesion().apply {
            activa(Sesion("token-ficticio", "almacen", "Prueba", setOf("CATALOGO_LEER")))
        }
        val cabeceras = mutableListOf<String?>()
        val cliente = crearHttpClient(MockEngine { solicitud ->
            cabeceras += solicitud.headers[HttpHeaders.Authorization]
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://ejemplo.local/api/v1/", loggerSilencioso, almacen, coordinador)

        try {
            cliente.get("productos")
            AuthRepositorioRest(cliente, almacen, coordinador).cerrarSesion()
            cliente.get("productos")
            assertEquals("Bearer token-ficticio", cabeceras.first())
            assertNull(cabeceras.last())
            assertNull(almacen.leerToken())
            assertNull(coordinador.estado.value.sesion)
        } finally { cliente.close() }
    }

    @Test
    fun logDeCabecerasNoExponeElToken() = runBlocking {
        val almacen = AlmacenMemoria("token-ficticio")
        val registros = mutableListOf<String>()
        val logger = object : Logger { override fun log(message: String) { registros += message } }
        val cliente = crearHttpClient(MockEngine {
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://ejemplo.local/api/v1/", logger, almacen, CoordinadorSesion(), esDepuracion = true)

        try {
            cliente.get("productos")
            assertTrue(registros.any { "Authorization" in it })
            assertTrue(registros.none { "token-ficticio" in it })
        } finally { cliente.close() }
    }

    @Test
    fun errorDeRedNoBorraTokenGuardado() = runBlocking {
        val almacen = AlmacenMemoria("token-ficticio")
        val coordinador = CoordinadorSesion()
        val cliente = crearHttpClient(MockEngine {
            throw java.io.IOException("sin red")
        }, "https://ejemplo.local/api/v1/", loggerSilencioso, almacen, coordinador)

        try {
            val error = assertFailsWith<ErrorSesionException> {
                AuthRepositorioRest(cliente, almacen, coordinador).sesionGuardada()
            }
            assertEquals(ErrorSesion.SinConexion, error.tipo)
            assertEquals("token-ficticio", almacen.leerToken())
        } finally { cliente.close() }
    }
}
