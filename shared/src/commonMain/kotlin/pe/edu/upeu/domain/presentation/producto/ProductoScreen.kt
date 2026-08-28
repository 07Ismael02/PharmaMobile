package pe.edu.upeu.domain.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.domain.model.Producto

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

    val nombreError = "El nombre es obligatorio."
    val precioNumericoError = "Ingrese un precio numérico."
    val precioRangoError = "El precio debe ser mayor que cero."
    val stockEnteroError = "Ingrese un stock entero."
    val stockRangoError = "El stock no puede ser negativo."

    fun registrarProducto() {
        intentoRegistrar = true

        if (!nombre.trim().isNotBlank()) {
            mensajeRetroalimentacion = nombreError
            return
        }

        val precioValor = precio.toDoubleOrNull()
        if (precioValor == null) {
            mensajeRetroalimentacion = precioNumericoError
            return
        }

        if (precioValor <= 0) {
            mensajeRetroalimentacion = precioRangoError
            return
        }

        val stockValor = stock.toIntOrNull()
        if (stockValor == null) {
            mensajeRetroalimentacion = stockEnteroError
            return
        }

        if (stockValor < 0) {
            mensajeRetroalimentacion = stockRangoError
            return
        }

        val producto = Producto(
            id = 0L,
            nombre = nombre.trim(),
            precio = precioValor,
            stock = stockValor
        )

        mensajeRetroalimentacion =
            "Producto \"${producto.nombre}\" registrado correctamente"
        nombre = ""
        precio = ""
        stock = ""
        intentoRegistrar = false
    }

    val nombreTieneError = intentoRegistrar && mensajeRetroalimentacion == nombreError
    val precioTieneError = intentoRegistrar && (
        mensajeRetroalimentacion == precioNumericoError ||
            mensajeRetroalimentacion == precioRangoError
        )
    val stockTieneError = intentoRegistrar && (
        mensajeRetroalimentacion == stockEnteroError ||
            mensajeRetroalimentacion == stockRangoError
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("PharmaMobil")
        Text("Registro de Producto")

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = {
                Text("Nombre")
            },
            isError = nombreTieneError,
            supportingText = {
                if (nombreTieneError) Text(nombreError)
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = {
                Text("Precio")
            },
            isError = precioTieneError,
            supportingText = {
                if (precioTieneError) mensajeRetroalimentacion?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = stock,
            onValueChange = { stock = it },
            label = {
                Text("Stock")
            },
            isError = stockTieneError,
            supportingText = {
                if (stockTieneError) mensajeRetroalimentacion?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth()
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
    }
}
