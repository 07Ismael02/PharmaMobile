package pe.edu.upeu.domain.usecase

import kotlinx.coroutines.CancellationException

suspend inline fun <T> resultadoDe(crossinline bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (cancelacion: CancellationException) {
    throw cancelacion
} catch (error: Exception) {
    Result.failure(error)
}
