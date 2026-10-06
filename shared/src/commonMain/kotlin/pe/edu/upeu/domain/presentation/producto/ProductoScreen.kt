package pe.edu.upeu.domain.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.domain.presentation.components.ValidatedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoScreen(viewModel: ProductoViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmarEliminacion by remember { mutableStateOf<Long?>(null) }
    val formulario = uiState.formulario
    val operando = uiState.operacion is Operacion.EnCurso
    val tabs = listOf("Activos", "Inactivos", "Bajo stock")
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("PharmaMobil")
        Text(if (uiState.editandoId == null) "Registro de Producto" else "Editar producto #${uiState.editandoId}")

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

        Text("Categoría")
        if (uiState.categorias.isEmpty()) {
            OutlinedButton(onClick = viewModel::cargarCategorias) { Text("Cargar categorías") }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.categorias.forEach { categoria ->
                    FilterChip(
                        selected = formulario.categoriaId == categoria.id,
                        onClick = { viewModel.onCategoriaSeleccionada(categoria.id) },
                        label = { Text(categoria.nombre) }
                    )
                }
            }
        }
        formulario.errorCategoria?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = if (uiState.editandoId == null) viewModel::registrar else viewModel::actualizar,
            enabled = !operando && uiState.categorias.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(when {
                uiState.guardando -> "Guardando..."
                uiState.editandoId != null -> "Actualizar"
                else -> "Registrar"
            })
        }

        if (uiState.editandoId != null) {
            OutlinedButton(onClick = viewModel::cancelarEdicion, enabled = !operando) { Text("Cancelar edición") }
        }

        uiState.mensaje?.let { Text(it) }
        uiState.mensajeExito?.let { Text(it) }
        (uiState.operacion as? Operacion.Fallida)?.let {
            Text(it.mensaje, color = MaterialTheme.colorScheme.error)
        }

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
            is Fase.ConProductos -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val productosFiltrados = when (uiState.tabSeleccionada) {
                    0 -> fase.productos.filter { it.activo }
                    1 -> fase.productos.filter { !it.activo }
                    else -> fase.productos.filter { it.requiereReposicion }
                }
                if (productosFiltrados.isEmpty()) {
                    item { Text("No hay productos en esta pestaña.") }
                }
                items(items = productosFiltrados, key = { it.id }) { producto ->
                    ProductoItem(
                        producto = producto,
                        operacion = uiState.operacion,
                        onEditar = { viewModel.editar(producto.id) },
                        onEliminar = { confirmarEliminacion = producto.id }
                    )
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

    confirmarEliminacion?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmarEliminacion = null },
            title = { Text("Dar de baja producto") },
            text = { Text("El producto pasará a Inactivos; no se borrará físicamente.") },
            confirmButton = {
                Button(onClick = {
                    confirmarEliminacion = null
                    viewModel.eliminar(id)
                }) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarEliminacion = null }) { Text("Volver") }
            }
        )
    }
}

@Composable
private fun ProductoItem(
    producto: ProductoUi,
    operacion: Operacion,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
            Text("Precio: S/ ${producto.precio}")
            Text("Stock: ${producto.stock}")
            producto.categoriaId?.let { Text("Categoría ID: $it") }
            Text(
                text = if (producto.activo) "Activo" else "Inactivo",
                color = if (producto.activo) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            if (operacion is Operacion.EnCurso && operacion.productoId == producto.id) {
                CircularProgressIndicator()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditar, enabled = operacion !is Operacion.EnCurso) {
                    Text("Editar")
                }
                if (producto.activo) {
                    OutlinedButton(onClick = onEliminar, enabled = operacion !is Operacion.EnCurso) {
                        Text("Dar de baja")
                    }
                }
            }
        }
    }
}
