package pe.edu.upeu.domain.repository

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

interface ProductoRepository {
    /** Los repositorios previos mantienen su contrato durante la transición del ViewModel. */
    fun observar(): Flow<List<Producto>> = flow { emit(listar()) }
    fun observarUltimaSincronizacion(): Flow<Long?> = flowOf(null)
    fun observarPendientes(): Flow<Int> = flowOf(0)
    fun observarFallidas(): Flow<Int> = flowOf(0)
    suspend fun sincronizar() { listar() }
    suspend fun registrar(producto: Producto): Producto
    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
    suspend fun listarCategorias(): List<Categoria>
}
