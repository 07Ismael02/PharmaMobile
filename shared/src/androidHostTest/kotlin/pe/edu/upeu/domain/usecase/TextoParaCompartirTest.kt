package pe.edu.upeu.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

class TextoParaCompartirTest {
    @Test
    fun textoIncluyeNombrePrecioFormateadoYStockSinAlterarDominio() {
        val producto = Producto(id = 1, nombre = "Paracetamol 500 mg", precio = 4.5, stock = 120)
        val precioOriginal = producto.precio

        val texto = producto.comoTextoParaCompartir()

        assertTrue(texto.contains(producto.nombre))
        assertTrue(texto.contains(formatearSoles(precioOriginal)))
        assertTrue(texto.contains("Stock: ${producto.stock}"))
        assertEquals(precioOriginal, producto.precio)
    }
}
