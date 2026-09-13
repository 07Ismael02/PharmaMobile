package pe.edu.upeu.domain.presentation.producto

import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase

object ProductoValidator {

    const val NOMBRE_ERROR = RegistrarProductoUseCase.NOMBRE_ERROR
    const val PRECIO_NUMERICO_ERROR = RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR
    const val PRECIO_RANGO_ERROR = RegistrarProductoUseCase.PRECIO_RANGO_ERROR
    const val STOCK_ENTERO_ERROR = RegistrarProductoUseCase.STOCK_ENTERO_ERROR
    const val STOCK_RANGO_ERROR = RegistrarProductoUseCase.STOCK_RANGO_ERROR

    fun validarNombre(nombre: String): String? {
        return RegistrarProductoUseCase.validarNombre(nombre)
    }

    fun validarPrecio(precio: String): String? {
        return RegistrarProductoUseCase.validarPrecio(precio)
    }

    fun validarStock(stock: String): String? {
        return RegistrarProductoUseCase.validarStock(stock)
    }
}
