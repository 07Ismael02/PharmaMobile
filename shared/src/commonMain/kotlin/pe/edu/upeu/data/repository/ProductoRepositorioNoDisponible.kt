package pe.edu.upeu.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

/** Estado cerrado: no existe base de productos activa antes de vincular una identidad validada. */
class ProductoRepositorioNoDisponible : ProductoRepository {
    private fun noDisponible(): Nothing = throw ErrorApiException(
        ErrorApi.Conflicto("El catálogo local no está disponible hasta vincular una cuenta validada.")
    )

    override fun observar(): Flow<List<Producto>> = flow { noDisponible() }
    override suspend fun sincronizar(): Unit = noDisponible()
    override suspend fun registrar(producto: Producto): Producto = noDisponible()
    override suspend fun listar(): List<Producto> = noDisponible()
    override suspend fun obtener(id: Long): Producto = noDisponible()
    override suspend fun actualizar(producto: Producto): Producto = noDisponible()
    override suspend fun eliminar(id: Long): Unit = noDisponible()
    override suspend fun listarCategorias(): List<Categoria> = noDisponible()
}
