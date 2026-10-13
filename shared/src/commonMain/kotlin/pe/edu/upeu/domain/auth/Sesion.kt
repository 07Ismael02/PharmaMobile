package pe.edu.upeu.domain.auth

data class Credenciales(val usuario: String, val clave: String) {
    override fun toString(): String = "Credenciales(usuario=$usuario, clave=[REDACTED])"
}

data class Sesion(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String>
) {
    fun puede(permiso: String): Boolean = permiso in permisos
    override fun toString(): String = "Sesion(usuario=$usuario, token=[REDACTED])"
}

sealed interface ErrorSesion {
    data object CredencialesInvalidas : ErrorSesion
    data object SesionExpirada : ErrorSesion
    data object SinPermiso : ErrorSesion
    data object SinConexion : ErrorSesion
}

class ErrorSesionException(val tipo: ErrorSesion) : Exception()
