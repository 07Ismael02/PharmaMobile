package pe.edu.upeu.domain.datasource

import pe.edu.upeu.domain.model.Producto
import kotlinx.coroutines.delay

val productosSimulados = listOf(
    Producto(id = 1L, nombre = "Paracetamol 500mg", precio = 2.50, stock = 50),
    Producto(id = 2L, nombre = "Ibuprofeno 400mg", precio = 3.80, stock = 30),
    Producto(id = 3L, nombre = "Amoxicilina 500mg", precio = 12.00, stock = 15)
)

suspend fun obtenerProductos(): List<Producto> {
    delay(1000)
    return productosSimulados
}

suspend fun buscarProductoPorId(id: Long): Producto? {
    delay(1000)
    return productosSimulados.find { it.id == id }
}