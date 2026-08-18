package com.edu.pe.domain.model

import pe.edu.upeu.domain.model.DetallePedido
import pe.edu.upeu.domain.model.EstadoPedido

data class Pedido(
    val id: Long,
    val cliente: Cliente,
    val detalles: List<DetallePedido>,
    val estado: EstadoPedido
)
