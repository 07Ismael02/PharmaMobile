package pe.edu.upeu.data.mapper

import pe.edu.upeu.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.domain.model.Producto

fun ProductoResponseDto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = estado
)
