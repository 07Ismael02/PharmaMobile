package pe.edu.upeu.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EstadoSesion(
    val cargando: Boolean = true,
    val sesion: Sesion? = null,
    val mensaje: String? = null
)

class CoordinadorSesion {
    private val _estado = MutableStateFlow(EstadoSesion())
    val estado = _estado.asStateFlow()

    fun activa(sesion: Sesion) {
        _estado.value = EstadoSesion(cargando = false, sesion = sesion)
    }

    fun sinSesion(mensaje: String? = null) {
        _estado.value = EstadoSesion(cargando = false, mensaje = mensaje)
    }

    fun expirada() {
        sinSesion("La sesión expiró. Inicie sesión nuevamente.")
    }
}
