package pe.edu.upeu.demo

import pe.edu.upeu.domain.model.Cliente

fun probarCliente(){
    val cliente = Cliente(
        id = 1L,
        nombre = "Farmacia Nueva Vida",
        correo = "ventas@central.pe",
        telefono = null
    )
    println(cliente.obtenerTelefono())
}