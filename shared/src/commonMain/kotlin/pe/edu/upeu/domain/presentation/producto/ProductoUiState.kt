package pe.edu.upeu.domain.presentation.producto

import pe.edu.upeu.domain.model.Producto

sealed interface Fase {
    data object Cargando : Fase
    data object SinProductos : Fase
    data object ConProductos : Fase
    data class Error(val detalle: String) : Fase
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null
)

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val productos: List<Producto> = emptyList(),
    val formulario: FormularioProducto = FormularioProducto(),
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val tabSeleccionada: Int = 0
)
