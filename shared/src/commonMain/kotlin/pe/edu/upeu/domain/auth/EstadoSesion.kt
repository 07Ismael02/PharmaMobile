package pe.edu.upeu.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EstadoSesion(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val mensaje: String? = null,
    val restauracionPendiente: Boolean = false,
    val generacion: Long = 0
)

class CoordinadorSesion {
    private val _estado = MutableStateFlow(EstadoSesion())
    val estado = _estado.asStateFlow()
    private var generacion = 0L

    fun activa(sesion: Sesion) {
        _estado.value = EstadoSesion(cargando = false, sesion = sesion, generacion = ++generacion)
    }

    fun sinSesion(mensaje: String? = null) {
        _estado.value = EstadoSesion(cargando = false, mensaje = mensaje, generacion = ++generacion)
    }

    fun restaurando() {
        _estado.value = _estado.value.copy(cargando = true, mensaje = null, restauracionPendiente = false)
    }

    fun comprobacionPendiente(mensaje: String) {
        _estado.value = _estado.value.copy(cargando = false, sesion = null,
            mensaje = mensaje,
            restauracionPendiente = true)
    }

    fun expirada() {
        sinSesion("La sesión ya no es válida. Inicie sesión nuevamente.")
    }
}
