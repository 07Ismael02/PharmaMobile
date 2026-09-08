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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import pe.edu.upeu.domain.datasource.productosSimulados
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.presentation.components.ValidatedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoScreen() {

    var nombre by remember {
        mutableStateOf("")
    }

    var precio by remember {
        mutableStateOf("")
    }

    var stock by remember {
        mutableStateOf("")
    }

    var mensajeRetroalimentacion by remember {
        mutableStateOf<String?>(null)
    }

    var intentoRegistrar by remember {
        mutableStateOf(false)
    }

    var productos by remember {
        mutableStateOf(productosSimulados)
    }

    var tabSeleccionada by remember {
        mutableStateOf(0)
    }

    fun registrarProducto() {
        intentoRegistrar = true

        val nombreError = ProductoValidator.validarNombre(nombre)
        if (nombreError != null) {
            mensajeRetroalimentacion = nombreError
            return
        }

        val precioError = ProductoValidator.validarPrecio(precio)
        if (precioError != null) {
            mensajeRetroalimentacion = precioError
            return
        }

        val precioValor = precio.toDoubleOrNull() ?: return

        val stockError = ProductoValidator.validarStock(stock)
        if (stockError != null) {
            mensajeRetroalimentacion = stockError
            return
        }

        val stockValor = stock.toIntOrNull() ?: return

        val producto = Producto(
            id = (productos.maxOfOrNull { it.id } ?: 0L) + 1L,
            nombre = nombre.trim(),
            precio = precioValor,
            stock = stockValor,
            activo = true
        )

        productos = productos + producto
        mensajeRetroalimentacion =
            "Producto \"${producto.nombre}\" registrado correctamente"
        nombre = ""
        precio = ""
        stock = ""
        intentoRegistrar = false
    }

    val nombreTieneError = intentoRegistrar &&
        mensajeRetroalimentacion == ProductoValidator.NOMBRE_ERROR
    val precioTieneError = intentoRegistrar && (
        mensajeRetroalimentacion == ProductoValidator.PRECIO_NUMERICO_ERROR ||
            mensajeRetroalimentacion == ProductoValidator.PRECIO_RANGO_ERROR
        )
    val stockTieneError = intentoRegistrar && (
        mensajeRetroalimentacion == ProductoValidator.STOCK_ENTERO_ERROR ||
            mensajeRetroalimentacion == ProductoValidator.STOCK_RANGO_ERROR
        )

    val productosFiltrados = when (tabSeleccionada) {
        0 -> productos.filter { it.activo }
        1 -> productos.filter { !it.activo }
        else -> productos.filter { it.stock <= 5 }
    }

    val tabs = listOf("Activos", "Inactivos", "Bajo stock")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("PharmaMobil")
        Text("Registro de Producto")

        ValidatedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre",
            error = if (nombreTieneError) mensajeRetroalimentacion else null
        )

        ValidatedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = "Precio",
            error = if (precioTieneError) mensajeRetroalimentacion else null
        )

        ValidatedTextField(
            value = stock,
            onValueChange = { stock = it },
            label = "Stock",
            error = if (stockTieneError) mensajeRetroalimentacion else null
        )

        Button(
            onClick = { registrarProducto() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        if (!intentoRegistrar) {
            mensajeRetroalimentacion?.let {
                Text(it)
            }
        }

        PrimaryTabRow(
            selectedTabIndex = tabSeleccionada
        ) {
            tabs.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = { Text(titulo) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = productosFiltrados,
                key = { it.id }
            ) { producto ->
                ProductoItem(producto)
            }
        }
    }
}

@Composable
private fun ProductoItem(producto: Producto) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Text("Precio: S/ ${producto.precio}")
            Text("Stock: ${producto.stock}")
            Text(
                text = if (producto.activo) "Activo" else "Inactivo",
                color = if (producto.activo) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}
