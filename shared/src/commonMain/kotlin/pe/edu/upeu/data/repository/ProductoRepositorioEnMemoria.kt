package pe.edu.upeu.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.domain.datasource.productosSimulados
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria : ProductoRepository {
    private val mutex = Mutex()
    private val productos = productosSimulados.toMutableList()

    override suspend fun registrar(producto: Producto): Producto {
        delay(500)
        return mutex.withLock {
            val siguienteId = (productos.maxOfOrNull { it.id } ?: 0L) + 1L
            val registrado = producto.copy(id = siguienteId)
            productos += registrado
            registrado
        }
    }

    override suspend fun listar(): List<Producto> {
        delay(500)
        return mutex.withLock { productos.toList() }
    }

    override suspend fun obtener(id: Long): Producto = mutex.withLock {
        productos.first { it.id == id }
    }

    override suspend fun actualizar(producto: Producto): Producto = mutex.withLock {
        val indice = productos.indexOfFirst { it.id == producto.id }
        require(indice >= 0) { "Producto no encontrado" }
        productos[indice] = producto
        producto
    }

    override suspend fun eliminar(id: Long) = mutex.withLock {
        val indice = productos.indexOfFirst { it.id == id }
        require(indice >= 0) { "Producto no encontrado" }
        productos[indice] = productos[indice].copy(activo = false)
    }

    override suspend fun listarCategorias(): List<Categoria> = emptyList()
}
