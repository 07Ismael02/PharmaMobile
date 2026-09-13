package pe.edu.upeu.domain.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.presentation.components.ValidatedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoScreen(viewModel: ProductoViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formulario = uiState.formulario
    val tabs = listOf("Activos", "Inactivos", "Bajo stock")
    val productosFiltrados = when (uiState.tabSeleccionada) {
        0 -> uiState.productos.filter { it.activo }
        1 -> uiState.productos.filter { !it.activo }
        else -> uiState.productos.filter { it.requiereReposicion }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("PharmaMobil")
        Text("Registro de Producto")

        ValidatedTextField(
            value = formulario.nombre,
            onValueChange = viewModel::onNombreChange,
            label = "Nombre",
            error = formulario.errorNombre
        )
        ValidatedTextField(
            value = formulario.precio,
            onValueChange = viewModel::onPrecioChange,
            label = "Precio",
            error = formulario.errorPrecio
        )
        ValidatedTextField(
            value = formulario.stock,
            onValueChange = viewModel::onStockChange,
            label = "Stock",
            error = formulario.errorStock
        )

        Button(
            onClick = viewModel::registrar,
            enabled = !uiState.guardando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.guardando) "Registrando..." else "Registrar")
        }

        uiState.mensaje?.let { Text(it) }

        PrimaryTabRow(selectedTabIndex = uiState.tabSeleccionada) {
            tabs.forEachIndexed { index, titulo ->
                Tab(
                    selected = uiState.tabSeleccionada == index,
                    onClick = { viewModel.onTabSeleccionada(index) },
                    text = { Text(titulo) }
                )
            }
        }

        when (val fase = uiState.fase) {
            Fase.Cargando -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("Cargando productos...")
            }
            Fase.SinProductos -> Text("No hay productos registrados.")
            Fase.ConProductos -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = productosFiltrados, key = { it.id }) { producto ->
                    ProductoItem(producto)
                }
            }
            is Fase.Error -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(fase.detalle, color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::cargarProductos) { Text("Reintentar") }
            }
        }
    }
}

@Composable
private fun ProductoItem(producto: Producto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Precio: S/ ${producto.precio}")
            Text("Stock: ${producto.stock}")
            Text(
                text = if (producto.activo) "Activo" else "Inactivo",
                color = if (producto.activo) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
