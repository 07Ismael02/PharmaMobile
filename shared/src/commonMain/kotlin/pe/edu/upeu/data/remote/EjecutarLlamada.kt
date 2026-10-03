package pe.edu.upeu.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import pe.edu.upeu.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException

/** Único límite entre errores de Ktor y errores propios de la aplicación. */
suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion
} catch (error: ClientRequestException) {
    Result.failure(ErrorApiException(traducirCliente(error)))
} catch (error: ServerResponseException) {
    Result.failure(ErrorApiException(ErrorApi.Servidor))
} catch (error: HttpRequestTimeoutException) {
    Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
} catch (error: IOException) {
    Result.failure(ErrorApiException(ErrorApi.SinConexion))
}

private suspend fun traducirCliente(error: ClientRequestException): ErrorApi {
    val cuerpo = try {
        error.response.body<ErrorResponseDto>()
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (_: Exception) {
        null
    }
    return when (error.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
}
