package pe.edu.upeu.domain.auth

class IniciarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke(credenciales: Credenciales): Sesion = repositorio.iniciarSesion(credenciales)
}

class RestaurarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke(): Sesion? = repositorio.sesionGuardada()
}

class CerrarSesionUseCase(private val repositorio: AuthRepository) {
    suspend operator fun invoke() = repositorio.cerrarSesion()
}
