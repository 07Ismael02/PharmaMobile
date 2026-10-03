package pe.edu.upeu.data.repository

import pe.edu.upeu.data.mapper.toDomain
import pe.edu.upeu.data.mapper.toRequest
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.ejecutarLlamada
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.repository.ProductoRepository

class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaApi: CategoriaApi
) : ProductoRepository {
    override suspend fun listar(): List<Producto> =
        ejecutarLlamada { api.listar().contenido.map { it.toDomain() } }.getOrThrow()

    override suspend fun obtener(id: Long): Producto =
        ejecutarLlamada { api.obtener(id).toDomain() }.getOrThrow()

    override suspend fun registrar(producto: Producto): Producto =
        ejecutarLlamada { api.crear(producto.toRequest()).toDomain() }.getOrThrow()

    override suspend fun actualizar(producto: Producto): Producto =
        ejecutarLlamada { api.actualizar(producto.id, producto.toRequest()).toDomain() }.getOrThrow()

    override suspend fun eliminar(id: Long) {
        ejecutarLlamada { api.eliminar(id) }.getOrThrow()
    }

    override suspend fun listarCategorias(): List<Categoria> =
        ejecutarLlamada { categoriaApi.listar().map { it.toDomain() } }.getOrThrow()
}
