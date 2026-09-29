package pe.edu.upeu.data.repository

import pe.edu.upeu.data.mapper.toDomain
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val respaldoLocal: ProductoRepositorioEnMemoria
) : ProductoRepository {
    override suspend fun listar(): List<Producto> =
        api.listar().contenido.map { it.toDomain() }

    // El alta REST corresponde a la sesión 8; en esta práctica se conserva el respaldo local.
    override suspend fun registrar(producto: Producto): Producto = respaldoLocal.registrar(producto)
}
