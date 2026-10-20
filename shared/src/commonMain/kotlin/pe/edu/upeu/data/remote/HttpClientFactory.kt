package pe.edu.upeu.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.data.auth.AlmacenSeguro
import pe.edu.upeu.domain.auth.CoordinadorSesion

fun crearHttpClient(
    engine: HttpClientEngine,
    urlBase: String,
    logger: Logger,
    almacen: AlmacenSeguro? = null,
    coordinador: CoordinadorSesion? = null,
    esDepuracion: Boolean = false,
    tokenFijo: String? = null,
    al401DeSesion: (() -> Unit)? = null,
    al401Global: ((String) -> Unit)? = null
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            this.logger = logger
            level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
        if (almacen != null && tokenFijo == null) {
            install(Auth) {
                bearer {
                    loadTokens { almacen.leerToken()?.let { BearerTokens(it, "") } }
                    sendWithoutRequest { request -> !request.url.toString().endsWith("/auth/login") }
                }
            }
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { fallo, request ->
                if (fallo is ClientRequestException &&
                    fallo.response.status == HttpStatusCode.Unauthorized &&
                    !request.url.toString().endsWith("/auth/login")) {
                    if (tokenFijo != null) {
                        al401DeSesion?.invoke()
                    } else if (almacen != null &&
                        request.headers[HttpHeaders.Authorization] == "Bearer ${almacen.leerToken()}") {
                        val tokenSolicitado = almacen.leerToken() ?: return@handleResponseExceptionWithRequest
                        almacen.borrarToken()
                        fallo.response.call.client.authProvider<BearerAuthProvider>()?.clearToken()
                        coordinador?.expirada()
                        al401Global?.invoke(tokenSolicitado)
                    }
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            url(urlBase)
            contentType(ContentType.Application.Json)
            // El cliente del ámbito no comparte la caché Auth ni reintenta POST al recibir 401.
            if (tokenFijo != null) header(HttpHeaders.Authorization, "Bearer $tokenFijo")
        }
    }
