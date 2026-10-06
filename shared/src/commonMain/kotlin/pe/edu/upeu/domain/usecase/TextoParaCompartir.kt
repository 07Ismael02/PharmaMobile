package pe.edu.upeu.domain.usecase

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

fun Producto.comoTextoParaCompartir(): String =
    "$nombre — ${formatearSoles(precio)} · Stock: $stock"
