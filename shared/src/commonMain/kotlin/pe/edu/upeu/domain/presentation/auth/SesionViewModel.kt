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
        coordinador.restaurando()
        viewModelScope.launch {
            try {
                val sesion = restaurarSesion()
                if (sesion != null) coordinador.activa(sesion)
                else if (coordinador.estado.value.cargando) coordinador.sinSesion()
            } catch (error: ErrorSesionException) {
                if (error.tipo == ErrorSesion.SinConexion) {
                    coordinador.comprobacionPendiente(
                        "Sin conexión. La sesión guardada se conserva; reintente para comprobarla.")
                } else if (error.tipo == ErrorSesion.SinPermiso) {
                    coordinador.comprobacionPendiente(
                        "El servidor denegó la comprobación de la sesión. Reintente o cierre sesión.")
                } else if (error.tipo == ErrorSesion.CuentaDistinta) {
                    coordinador.sinSesion("Esta instalación está vinculada a otra cuenta.")
                } else if (error.tipo == ErrorSesion.AlmacenNoDisponible) {
                    coordinador.sinSesion("No se pudo abrir el almacenamiento protegido. Reintente.")
                } else {
                    coordinador.sinSesion("La sesión ya no es válida. Inicie sesión nuevamente.")
                }
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
                    ErrorSesion.CuentaDistinta -> "Esta instalación está vinculada a otra cuenta."
                    ErrorSesion.AlmacenNoDisponible -> "No se pudo abrir el almacenamiento protegido. Reintente."
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
