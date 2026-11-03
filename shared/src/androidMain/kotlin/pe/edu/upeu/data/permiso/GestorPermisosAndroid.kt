package pe.edu.upeu.data.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import pe.edu.upeu.domain.permiso.EstadoPermiso
import pe.edu.upeu.domain.permiso.GestorPermisos
import pe.edu.upeu.domain.permiso.Permiso
import pe.edu.upeu.domain.permiso.resolverEstadoPermiso

/** La Activity registra el puente antes de mostrar Compose. */
interface PuentePermisos {
    fun mostrarExplicacion(nombre: String): Boolean
    suspend fun pedir(nombre: String): Boolean
}

class GestorPermisosAndroid(private val contexto: Context) : GestorPermisos {
    private val preferencias = contexto.getSharedPreferences("permisos_pharmamobil", Context.MODE_PRIVATE)
    var puente: PuentePermisos? = null

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        if (Build.VERSION.SDK_INT < 33) return EstadoPermiso.Concedido
        val nombre = Manifest.permission.POST_NOTIFICATIONS
        val concedido = contexto.checkSelfPermission(nombre) == PackageManager.PERMISSION_GRANTED
        val pedido = preferencias.getBoolean("pedido_$nombre", false)
        val explicacion = puente?.mostrarExplicacion(nombre) ?: true
        return resolverEstadoPermiso(concedido, pedido, explicacion)
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        if (Build.VERSION.SDK_INT < 33) return EstadoPermiso.Concedido
        val activo = puente ?: return EstadoPermiso.Denegado
        val nombre = Manifest.permission.POST_NOTIFICATIONS
        preferencias.edit().putBoolean("pedido_$nombre", true).apply()
        return if (activo.pedir(nombre)) EstadoPermiso.Concedido else estado(permiso)
    }

    override fun abrirAjustes() {
        contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${contexto.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
