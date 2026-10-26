package pe.edu.upeu

import kotlin.test.Test
import kotlin.test.assertEquals
import pe.edu.upeu.domain.model.EstadoProductoLocal
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.presentation.producto.toUi

class PresentacionS11Test {
    @Test
    fun estadoPendienteYEnRevisionLlegaALaPresentacion() {
        val producto = Producto(-1, "Producto local", 4.5, 3)
        assertEquals(EstadoProductoLocal.PENDIENTE,
            producto.copy(estadoLocal = EstadoProductoLocal.PENDIENTE).toUi().estadoLocal)
        assertEquals(EstadoProductoLocal.EN_REVISION,
            producto.copy(estadoLocal = EstadoProductoLocal.EN_REVISION).toUi().estadoLocal)
    }

    @Test
    fun saludoUsaRolesConfirmadosSinAsignarRolPorDefecto() {
        assertEquals("Administrador", denominacionRol(setOf("ADMIN")))
        assertEquals("Vendedor", denominacionRol(setOf("VENDEDOR")))
        assertEquals("Almacenero", denominacionRol(setOf("ALMACENERO")))
        assertEquals(null, denominacionRol(emptySet()))
    }
}
