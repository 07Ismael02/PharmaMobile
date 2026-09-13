package pe.edu.upeu.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductoTest {
    @Test
    fun requiereReposicionConStockMenorOIgualACinco() {
        assertTrue(Producto(1, "A", 1.0, 5).requiereReposicion)
        assertTrue(Producto(2, "B", 1.0, 0, activo = false).requiereReposicion)
        assertFalse(Producto(3, "C", 1.0, 6).requiereReposicion)
    }
}
