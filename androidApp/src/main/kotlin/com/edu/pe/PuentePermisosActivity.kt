package com.edu.pe

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CompletableDeferred
import pe.edu.upeu.data.permiso.PuentePermisos

class PuentePermisosActivity(private val actividad: ComponentActivity) : PuentePermisos {
    private var pendiente: CompletableDeferred<Boolean>? = null
    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    override fun mostrarExplicacion(nombre: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(actividad, nombre)

    override suspend fun pedir(nombre: String): Boolean {
        check(pendiente == null) { "Ya hay una solicitud de permiso en curso" }
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        return try {
            lanzador.launch(nombre)
            espera.await()
        } finally {
            pendiente = null
        }
    }
}
