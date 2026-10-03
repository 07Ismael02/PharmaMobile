package pe.edu.upeu.domain.repository

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria

interface ProductoRepository {
    suspend fun registrar(producto: Producto): Producto
    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
    suspend fun listarCategorias(): List<Categoria>
}
