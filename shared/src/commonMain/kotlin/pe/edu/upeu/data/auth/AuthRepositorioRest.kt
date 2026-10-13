package pe.edu.upeu.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
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

class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro,
    private val coordinador: CoordinadorSesion
) : AuthRepository {
    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion = try {
        val dto: LoginRespuestaDto = cliente.post("auth/login") {
            setBody(LoginPeticionDto(credenciales.usuario, credenciales.clave))
        }.body()
        require(dto.tipo == "Bearer" && dto.token.isNotBlank())
        almacen.guardarToken(dto.token)
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        Sesion(dto.token, dto.username, dto.nombreCompleto, dto.permisos.toSet()).also(coordinador::activa)
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
            Sesion(token, dto.username, dto.nombreCompleto, dto.permisos.toSet()).also(coordinador::activa)
        } catch (cancelacion: CancellationException) {
            throw cancelacion
        } catch (error: ClientRequestException) {
            if (error.response.status == HttpStatusCode.Unauthorized) {
                almacen.borrarToken()
                cliente.authProvider<BearerAuthProvider>()?.clearToken()
                coordinador.expirada()
                null
            } else throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (_: IOException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        cliente.authProvider<BearerAuthProvider>()?.clearToken()
        coordinador.sinSesion()
    }
}
