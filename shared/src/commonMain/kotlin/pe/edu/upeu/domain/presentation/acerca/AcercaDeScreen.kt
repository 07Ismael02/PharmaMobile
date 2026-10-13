package pe.edu.upeu.domain.presentation.acerca

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.platform.InfoDispositivo

@Composable
fun AcercaDeScreen() {
    val dispositivo = remember { InfoDispositivo() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Acerca de PharmaMobil", style = MaterialTheme.typography.headlineSmall)
        Text("Sistema operativo: ${dispositivo.sistema}")
        Text("Versión: ${dispositivo.version}")
    }
}
