package pe.edu.upeu.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import pe.edu.upeu.domain.auth.*

@Serializable
private data class LoginPeticionDto(val username: String, val password: String)

@Serializable
private data class LoginRespuestaDto(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>
)

@Serializable
private data class IdentidadDto(
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>
)

/** Se serializa únicamente dentro del almacén protegido, nunca en logs ni preferencias legibles. */
@Serializable
private data class IdentidadLocalValidada(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String>,
    val roles: Set<String>
)

class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro,
    private val coordinador: CoordinadorSesion,
    private val ambitos: CoordinadorAmbitoProductos? = null
) : AuthRepository {
    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion = try {
        val dto: LoginRespuestaDto = cliente.post("auth/login") {
            setBody(LoginPeticionDto(credenciales.usuario, credenciales.clave))
        }.body()
        require(dto.tipo == "Bearer" && dto.token.isNotBlank())
        almacen.guardarToken(dto.token)
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        val sesion = Sesion(dto.token, dto.username, dto.nombreCompleto,
            dto.permisos.toSet(), dto.roles.toSet())
        activarValidada(sesion).also(::guardarIdentidadConfirmada)
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (error: ClientRequestException) {
        throw ErrorSesionException(if (error.response.status == HttpStatusCode.Unauthorized)
            ErrorSesion.CredencialesInvalidas else if (error.response.status == HttpStatusCode.Forbidden)
            ErrorSesion.SinPermiso else ErrorSesion.SinConexion)
    } catch (_: HttpRequestTimeoutException) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    } catch (_: IOException) {
        throw ErrorSesionException(ErrorSesion.SinConexion)
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        return try {
            val dto: IdentidadDto = cliente.get("auth/me").body()
            val sesion = Sesion(token, dto.username, dto.nombreCompleto,
                dto.permisos.toSet(), dto.roles.toSet())
            activarValidada(sesion).also(::guardarIdentidadConfirmada)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (error: ClientRequestException) {
            if (error.response.status == HttpStatusCode.Unauthorized) {
                ambitos?.revocarSiToken(token)
                if (almacen.leerToken() == token) {
                    almacen.borrarToken()
                    almacen.borrarIdentidadValidada()
                    cliente.authProvider<BearerAuthProvider>()?.clearToken()
                    coordinador.expirada()
                }
                null
            } else if (error.response.status == HttpStatusCode.Forbidden) {
                throw ErrorSesionException(ErrorSesion.SinPermiso)
            } else throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (_: HttpRequestTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (_: ServerResponseException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (_: IOException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        }
    }

    override suspend fun sesionOfflineGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        val identidad = leerIdentidadConfirmada() ?: return null
        if (identidad.token != token || identidad.usuario.isBlank() ||
            "CATALOGO_LEER" !in identidad.permisos) return null
        // Incluso si el JWT venció, este ámbito local no posee cliente HTTP ni permite escritura.
        // Al volver la red, /auth/me decide si la sesión puede continuar.
        val sesion = Sesion(token, identidad.usuario, identidad.nombreCompleto,
            identidad.permisos, identidad.roles)
        return if (ambitos?.activarOffline(sesion) == true) sesion else null
    }

    override suspend fun backendDisponibleSinSesion(): Boolean = try {
        cliente.get("auth/me")
        true
    } catch (_: ClientRequestException) {
        true // 401 significa que el servidor sí responde; no hay sesión aún.
    } catch (_: ServerResponseException) {
        true
    } catch (_: HttpRequestTimeoutException) {
        false
    } catch (_: IOException) {
        false
    }

    override suspend fun cerrarSesion() {
        ambitos?.revocar()
        almacen.borrarToken()
        almacen.borrarIdentidadValidada()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        coordinador.sinSesion()
    }

    private suspend fun activarValidada(sesion: Sesion): Sesion {
        try {
            if (ambitos?.activar(sesion) == false) {
                throw ErrorSesionException(ErrorSesion.CuentaDistinta)
            }
        } catch (cancelacion: CancellationException) {
            withContext(NonCancellable) {
                ambitos?.revocarSiToken(sesion.token)
                if (almacen.leerToken() == sesion.token) almacen.borrarToken()
            }
            throw cancelacion
        } catch (error: Exception) {
            // Un intento de cambio de cuenta fallido no debe dejar visible el ámbito anterior.
            ambitos?.revocar()
            if (almacen.leerToken() == sesion.token) {
                almacen.borrarToken()
                cliente.authProvider<BearerAuthProvider>()?.clearToken()
            }
            coordinador.sinSesion()
            throw if (error is ErrorSesionException) error
            else ErrorSesionException(ErrorSesion.AlmacenNoDisponible)
        }
        return sesion.also(coordinador::activa)
    }

    private fun leerIdentidadConfirmada(): IdentidadLocalValidada? = runCatching {
        val datos = almacen.leerIdentidadValidada() ?: return null
        Json.decodeFromString<IdentidadLocalValidada>(datos)
    }.getOrNull()

    private fun guardarIdentidadConfirmada(sesion: Sesion) {
        // Un fallo del almacén no afecta al login online: solo deshabilita la restauración offline.
        runCatching {
            almacen.borrarIdentidadValidada()
            almacen.guardarIdentidadValidada(Json.encodeToString(
                IdentidadLocalValidada(sesion.token, sesion.usuario, sesion.nombreCompleto,
                    sesion.permisos, sesion.roles)
            ))
        }
    }
}
