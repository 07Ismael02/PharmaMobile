package pe.edu.upeu.domain.presentation.producto

import pe.edu.upeu.domain.model.Producto

data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean,
    val categoriaId: Long?,
    val requiereReposicion: Boolean
)

fun Producto.toUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = activo,
    categoriaId = categoriaId,
    requiereReposicion = requiereReposicion
)
