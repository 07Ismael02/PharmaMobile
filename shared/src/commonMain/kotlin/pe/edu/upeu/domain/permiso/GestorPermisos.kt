package pe.edu.upeu.domain.permiso

enum class Permiso { NOTIFICACIONES }

sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    data object Denegado : EstadoPermiso
    data object DenegadoParaSiempre : EstadoPermiso
}

interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}

/** Evita confundir la primera solicitud con una denegación permanente. */
fun resolverEstadoPermiso(concedido: Boolean, yaSePidio: Boolean, procedeExplicar: Boolean): EstadoPermiso =
    when {
        concedido -> EstadoPermiso.Concedido
        !yaSePidio || procedeExplicar -> EstadoPermiso.Denegado
        else -> EstadoPermiso.DenegadoParaSiempre
    }
