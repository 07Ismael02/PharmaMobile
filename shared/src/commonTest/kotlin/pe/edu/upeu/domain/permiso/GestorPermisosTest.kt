package pe.edu.upeu.domain.permiso

import kotlin.test.Test
import kotlin.test.assertEquals

class GestorPermisosTest {
    @Test
    fun primeraSolicitudNoEsDenegacionPermanente() {
        assertEquals(EstadoPermiso.Denegado, resolverEstadoPermiso(false, false, false))
    }

    @Test
    fun denegacionConExplicacionPermiteVolverAPedir() {
        assertEquals(EstadoPermiso.Denegado, resolverEstadoPermiso(false, true, true))
    }

    @Test
    fun denegacionSinExplicacionTrasPedirEsPermanente() {
        assertEquals(EstadoPermiso.DenegadoParaSiempre, resolverEstadoPermiso(false, true, false))
    }

    @Test
    fun concesionTienePrioridad() {
        assertEquals(EstadoPermiso.Concedido, resolverEstadoPermiso(true, true, false))
    }
}
