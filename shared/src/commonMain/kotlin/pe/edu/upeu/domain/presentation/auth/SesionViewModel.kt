package pe.edu.upeu.domain.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.domain.auth.*

class SesionViewModel(
    private val iniciarSesion: IniciarSesionUseCase,
    private val restaurarSesion: RestaurarSesionUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val coordinador: CoordinadorSesion
) : ViewModel() {
    val estado = coordinador.estado
    private val _procesando = MutableStateFlow(false)
    val procesando = _procesando.asStateFlow()

    init { restaurar() }

    fun restaurar() {
        viewModelScope.launch {
            try {
                val sesion = restaurarSesion()
                if (sesion != null) coordinador.activa(sesion)
                else if (coordinador.estado.value.cargando) coordinador.sinSesion()
            } catch (_: ErrorSesionException) {
                coordinador.sinSesion("No se pudo comprobar la sesión. Verifique la conexión.")
            }
        }
    }

    fun iniciar(usuario: String, clave: String) {
        if (_procesando.value) return
        if (usuario.isBlank() || clave.isBlank()) {
            coordinador.sinSesion("Ingrese usuario y contraseña.")
            return
        }
        viewModelScope.launch {
            _procesando.value = true
            try {
                iniciarSesion(Credenciales(usuario.trim(), clave))
            } catch (error: ErrorSesionException) {
                coordinador.sinSesion(when (error.tipo) {
                    ErrorSesion.CredencialesInvalidas -> "Credenciales inválidas."
                    ErrorSesion.SinPermiso -> "No tiene permiso para acceder."
                    ErrorSesion.SesionExpirada -> "La sesión expiró."
                    ErrorSesion.SinConexion -> "Sin conexión con el backend. Reintente."
                })
            } finally {
                _procesando.value = false
            }
        }
    }

    fun cerrar() {
        viewModelScope.launch { cerrarSesion() }
    }
}
