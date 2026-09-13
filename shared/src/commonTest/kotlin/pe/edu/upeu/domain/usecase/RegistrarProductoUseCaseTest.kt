package pe.edu.upeu.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegistrarProductoUseCaseTest {
    @Test
    fun validaNombreObligatorio() {
        assertEquals(
            RegistrarProductoUseCase.NOMBRE_ERROR,
            RegistrarProductoUseCase.validarNombre("   ")
        )
    }

    @Test
    fun validaPrecioNumericoFinitoYMayorACero() {
        assertEquals(RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR, RegistrarProductoUseCase.validarPrecio("abc"))
        assertEquals(RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR, RegistrarProductoUseCase.validarPrecio("NaN"))
        assertEquals(RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR, RegistrarProductoUseCase.validarPrecio("Infinity"))
        assertEquals(RegistrarProductoUseCase.PRECIO_RANGO_ERROR, RegistrarProductoUseCase.validarPrecio("0"))
        assertEquals(RegistrarProductoUseCase.PRECIO_RANGO_ERROR, RegistrarProductoUseCase.validarPrecio("-1"))
        assertNull(RegistrarProductoUseCase.validarPrecio("10.50"))
    }

    @Test
    fun validaStockEnteroNoNegativoYPermiteCero() {
        assertEquals(RegistrarProductoUseCase.STOCK_ENTERO_ERROR, RegistrarProductoUseCase.validarStock("abc"))
        assertEquals(RegistrarProductoUseCase.STOCK_RANGO_ERROR, RegistrarProductoUseCase.validarStock("-1"))
        assertNull(RegistrarProductoUseCase.validarStock("0"))
    }
}
