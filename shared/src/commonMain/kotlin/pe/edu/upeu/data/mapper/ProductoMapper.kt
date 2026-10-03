package pe.edu.upeu.data.mapper

import pe.edu.upeu.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.data.remote.dto.CategoriaResponseDto
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto

fun ProductoResponseDto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = estado,
    categoriaId = categoriaId
)

fun Producto.toRequest(): ProductoRequestDto {
    val idCategoria = categoriaId ?: throw ErrorApiException(
        ErrorApi.Validacion(mapOf("categoriaId" to "Seleccione una categoría válida."))
    )
    return ProductoRequestDto(nombre, precio, stock, activo, idCategoria)
}

fun CategoriaResponseDto.toDomain(): Categoria = Categoria(id, nombre, estado)
