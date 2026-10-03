package pe.edu.upeu.domain.usecase

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

class ActualizarProductoUseCase(private val repository: ProductoRepository) {
    suspend operator fun invoke(producto: Producto) = resultadoDe { repository.actualizar(producto) }
}
