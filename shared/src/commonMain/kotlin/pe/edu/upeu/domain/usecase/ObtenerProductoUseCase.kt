package pe.edu.upeu.domain.usecase

import pe.edu.upeu.domain.repository.ProductoRepository

class ObtenerProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(id: Long) = resultadoDe { repository.obtener(id) }
}
