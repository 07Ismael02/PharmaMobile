package pe.edu.upeu.domain.presentation.producto

object ProductoValidator {

    const val NOMBRE_ERROR = "El nombre es obligatorio."
    const val PRECIO_NUMERICO_ERROR = "Ingrese un precio numérico."
    const val PRECIO_RANGO_ERROR = "El precio debe ser mayor que cero."
    const val STOCK_ENTERO_ERROR = "Ingrese un stock entero."
    const val STOCK_RANGO_ERROR = "El stock no puede ser negativo."

    fun validarNombre(nombre: String): String? {
        return if (nombre.trim().isNotBlank()) null else NOMBRE_ERROR
    }

    fun validarPrecio(precio: String): String? {
        val precioValor = precio.toDoubleOrNull()
        return when {
            precioValor == null || !precioValor.isFinite() -> PRECIO_NUMERICO_ERROR
            precioValor <= 0 -> PRECIO_RANGO_ERROR
            else -> null
        }
    }

    fun validarStock(stock: String): String? {
        val stockValor = stock.toIntOrNull()
        return when {
            stockValor == null -> STOCK_ENTERO_ERROR
            stockValor < 0 -> STOCK_RANGO_ERROR
            else -> null
        }
    }
}
