package pe.edu.upeu.pharmamobil.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.presentation.producto.toUi

class FormatoAndroidTest {
    @Test
    fun formateaMonedaPeruanaSinDependerDeEspaciosUnicode() {
        val resultado = formatearSoles(4.5)
        val compacto = resultado.filterNot { it.isWhitespace() || Character.isSpaceChar(it) }

        assertTrue(compacto.contains("S/") || compacto.contains("PEN"), resultado)
        assertTrue(compacto.contains("4.50") || compacto.contains("4,50"), resultado)
    }

    @Test
    fun mapperConservaPrecioNumericoYPreparaPrecioFormateado() {
        val producto = Producto(id = 1, nombre = "Paracetamol", precio = 4.5, stock = 10)

        val ui = producto.toUi()

        assertEquals(4.5, ui.precio)
        assertEquals(formatearSoles(producto.precio), ui.precioFormateado)
        val compacto = ui.precioFormateado.filterNot { it.isWhitespace() || Character.isSpaceChar(it) }
        assertTrue(compacto.contains("S/") || compacto.contains("PEN"), ui.precioFormateado)
        assertTrue(compacto.contains("4.50") || compacto.contains("4,50"), ui.precioFormateado)
    }
}
