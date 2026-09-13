package pe.edu.upeu.domain.repository

import pe.edu.upeu.domain.model.Producto

interface ProductoRepository {
    suspend fun registrar(producto: Producto): Producto
    suspend fun listar(): List<Producto>
}
