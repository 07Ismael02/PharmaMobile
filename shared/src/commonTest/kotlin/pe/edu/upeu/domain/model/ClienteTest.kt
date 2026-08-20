package pe.edu.upeu.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ClienteTest {
    @Test
    fun probarCliente() {

        val cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Nueva Vida",
            correo = "ventas@central.pe",
            telefono = "905530839"
        )
        val resultado = cliente.obtenerTelefono()

        assertEquals(
            "905530839",
            resultado
        )
    }
}
