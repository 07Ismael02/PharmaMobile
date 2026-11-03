package pe.edu.upeu.domain.presentation.acerca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.edu.upeu.domain.permiso.EstadoPermiso
import pe.edu.upeu.domain.permiso.GestorPermisos
import pe.edu.upeu.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.platform.InfoDispositivo

@Composable
fun AcercaDeScreen() {
    val dispositivo = remember { InfoDispositivo() }
    val gestor = koinInject<GestorPermisos>()
    val alcance = rememberCoroutineScope()
    var estado by remember { mutableStateOf<EstadoPermiso>(EstadoPermiso.Denegado) }
    LaunchedEffect(Unit) { estado = gestor.estado(Permiso.NOTIFICACIONES) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Acerca de PharmaMobil", style = MaterialTheme.typography.headlineSmall)
        Text("Sistema operativo: ${dispositivo.sistema}")
        Text("Versión: ${dispositivo.version}")
        if (dispositivo.sistema == "Android") {
            Text("Permiso de notificaciones: " + when (estado) {
                EstadoPermiso.Concedido -> "Concedido"
                EstadoPermiso.Denegado -> "Denegado"
                EstadoPermiso.DenegadoParaSiempre -> "Denegado permanentemente"
            })
            Text("Puede autorizar notificaciones del sistema. El catálogo funciona sin este permiso.")
            OutlinedButton(onClick = {
                alcance.launch { estado = gestor.estado(Permiso.NOTIFICACIONES) }
            }) { Text("Comprobar estado") }
            when (estado) {
                EstadoPermiso.Denegado -> Button(onClick = {
                    alcance.launch { estado = gestor.solicitar(Permiso.NOTIFICACIONES) }
                }) { Text("Solicitar permiso") }
                EstadoPermiso.DenegadoParaSiempre -> OutlinedButton(onClick = gestor::abrirAjustes) {
                    Text("Abrir Ajustes")
                }
                EstadoPermiso.Concedido -> Unit
            }
        }
    }
}
