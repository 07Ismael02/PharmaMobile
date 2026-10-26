package pe.edu.upeu.domain.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals

class AntiguedadDatosTest {
    @Test fun nuncaSincronizado() = assertEquals("Nunca sincronizado", antiguedadDatos(null, 100_000))
    @Test fun minutosHorasYDias() {
        val ahora = 200_000L
        assertEquals("Actualizado hace 2 minutos", antiguedadDatos(ahora - 120, ahora))
        assertEquals("Actualizado hace 2 horas", antiguedadDatos(ahora - 7_200, ahora))
        assertEquals("Actualizado hace 3 días", antiguedadDatos(ahora - 259_200, ahora))
    }
}
