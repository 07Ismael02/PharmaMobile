package pe.edu.upeu.domain.auth

interface AuthRepository {
    suspend fun iniciarSesion(credenciales: Credenciales): Sesion
    suspend fun sesionGuardada(): Sesion?
    suspend fun sesionOfflineGuardada(): Sesion? = null
    suspend fun backendDisponibleSinSesion(): Boolean = true
    suspend fun cerrarSesion()
}
