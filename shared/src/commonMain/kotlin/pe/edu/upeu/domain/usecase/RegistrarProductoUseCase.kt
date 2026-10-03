package pe.edu.upeu.domain.usecase

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository

class RegistrarProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(nombre: String, precio: String, stock: String, categoriaId: Long? = null): Result<Producto> =
        resultadoDe {
            validarNombre(nombre)?.let { error(it) }
            validarPrecio(precio)?.let { error(it) }
            validarStock(stock)?.let { error(it) }

            val nombreLimpio = nombre.trim()
            val precioValor = requireNotNull(precio.toDoubleOrNull())
            val stockValor = requireNotNull(stock.toIntOrNull())

            repository.registrar(
                Producto(
                    id = 0L,
                    nombre = nombreLimpio,
                    precio = precioValor,
                    stock = stockValor,
                    activo = true,
                    categoriaId = categoriaId
                )
            )
        }

    companion object {
        const val NOMBRE_ERROR = "El nombre es obligatorio."
        const val PRECIO_NUMERICO_ERROR = "Ingrese un precio numérico."
        const val PRECIO_RANGO_ERROR = "El precio debe ser mayor que cero."
        const val STOCK_ENTERO_ERROR = "Ingrese un stock entero."
        const val STOCK_RANGO_ERROR = "El stock no puede ser negativo."

        fun validarNombre(nombre: String): String? =
            if (nombre.trim().isNotBlank()) null else NOMBRE_ERROR

        fun validarPrecio(precio: String): String? {
            val valor = precio.toDoubleOrNull()
            return when {
                valor == null || !valor.isFinite() -> PRECIO_NUMERICO_ERROR
                valor <= 0 -> PRECIO_RANGO_ERROR
                else -> null
            }
        }

        fun validarStock(stock: String): String? {
            val valor = stock.toIntOrNull()
            return when {
                valor == null -> STOCK_ENTERO_ERROR
                valor < 0 -> STOCK_RANGO_ERROR
                else -> null
            }
        }
    }
}
