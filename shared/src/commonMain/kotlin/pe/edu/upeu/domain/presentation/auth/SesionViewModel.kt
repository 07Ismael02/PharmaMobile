package pe.edu.upeu.domain.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
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
    private var revalidacion: Job? = null

    init { restaurar() }

    fun restaurar() {
        revalidacion?.cancel()
        coordinador.restaurando()
        viewModelScope.launch {
            try {
                val sesion = restaurarSesion()
                if (sesion != null) coordinador.activa(sesion)
                else if (coordinador.estado.value.cargando) {
                    coordinador.sinSesion(if (restaurarSesion.backendDisponibleSinSesion()) null
                        else "Necesitas conexión a internet para iniciar sesión por primera vez.")
                }
            } catch (error: ErrorSesionException) {
                if (error.tipo == ErrorSesion.SinConexion) {
                    val offline = try { restaurarSesion.offline() }
                    catch (expirada: ErrorSesionException) {
                        if (expirada.tipo == ErrorSesion.SesionExpirada) {
                            coordinador.sinSesion("La sesión expiró. Conéctese e inicie sesión nuevamente.")
                            return@launch
                        }
                        null
                    }
                    if (offline != null) {
                        coordinador.activaOffline(offline)
                        iniciarRevalidacion()
                    } else coordinador.comprobacionPendiente(
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
        revalidacion?.cancel()
        viewModelScope.launch { cerrarSesion() }
    }

    private fun iniciarRevalidacion() {
        revalidacion?.cancel()
        revalidacion = viewModelScope.launch {
            while (coordinador.estado.value.accesoOffline) {
                delay(15_000)
                try {
                    val sesion = restaurarSesion()
                    if (sesion == null) {
                        coordinador.sinSesion("La sesión ya no es válida. Inicie sesión nuevamente.")
                        break
                    }
                    coordinador.activa(sesion)
                    break
                } catch (error: ErrorSesionException) {
                    if (error.tipo == ErrorSesion.SinConexion ||
                        error.tipo == ErrorSesion.SinPermiso) continue
                    cerrarSesion()
                    coordinador.sinSesion("La sesión ya no es válida. Inicie sesión nuevamente.")
                    break
                }
            }
        }
    }
}
