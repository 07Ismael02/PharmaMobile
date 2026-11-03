package pe.edu.upeu.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.data.local.CategoriaLocal
import pe.edu.upeu.data.local.EstadoSyncLocal
import pe.edu.upeu.data.local.ProductoLocal
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

/** No posee cliente HTTP: un JWT sin revalidar jamás autoriza operaciones remotas. */
class ProductoRepositorioSoloLectura(
    private val productos: ProductoLocal,
    private val categorias: CategoriaLocal,
    private val estadoSync: EstadoSyncLocal
) : ProductoRepository {
    override fun observar(): Flow<List<Producto>> = productos.observar()
    override fun observarUltimaSincronizacion(): Flow<Long?> = estadoSync.observarUltimaSincronizacion()
    override fun observarPendientes(): Flow<Int> = productos.observarPendientes()
    override fun observarFallidas(): Flow<Int> = productos.observarFallidas()
    override suspend fun listar(): List<Producto> = productos.listar()
    override suspend fun obtener(id: Long): Producto = productos.buscar(id)
        ?: throw ErrorApiException(ErrorApi.NoEncontrado)
    override suspend fun listarCategorias(): List<Categoria> = categorias.listar()
    override suspend fun sincronizar(): Unit = sinConexion()
    override suspend fun registrar(producto: Producto): Producto = sinConexion()
    override suspend fun actualizar(producto: Producto): Producto = sinConexion()
    override suspend fun eliminar(id: Long): Unit = sinConexion()

    private fun sinConexion(): Nothing = throw ErrorApiException(ErrorApi.SinConexion)
}
