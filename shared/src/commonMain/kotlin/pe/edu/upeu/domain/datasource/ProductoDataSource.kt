package pe.edu.upeu.domain.datasource

import pe.edu.upeu.domain.model.Producto
import kotlinx.coroutines.delay

val productosSimulados = listOf(
    Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100),
    Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50),
    Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5),
    Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
    Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3)
)

suspend fun obtenerProductos(): List<Producto> {
    delay(1000)
    return productosSimulados
}

suspend fun buscarProductoPorId(id: Long): Producto? {
    delay(1000)
    return productosSimulados.find { it.id == id }
}
