package pe.edu.upeu.domain.usecase

import pe.edu.upeu.domain.repository.ProductoRepository

class ListarProductosUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke() = resultadoDe { repository.listar() }
    fun observar() = repository.observar()
    fun observarUltimaSincronizacion() = repository.observarUltimaSincronizacion()
    suspend fun sincronizar() = resultadoDe { repository.sincronizar() }
}
