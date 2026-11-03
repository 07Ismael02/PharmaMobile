package pe.edu.upeu.data.permiso

import pe.edu.upeu.domain.permiso.EstadoPermiso
import pe.edu.upeu.domain.permiso.GestorPermisos
import pe.edu.upeu.domain.permiso.Permiso

/** Las notificaciones iOS requieren una integración nativa posterior. */
class GestorPermisosIosNoDisponible : GestorPermisos {
    override suspend fun estado(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override suspend fun solicitar(permiso: Permiso): EstadoPermiso = EstadoPermiso.Denegado
    override fun abrirAjustes() = Unit
}
