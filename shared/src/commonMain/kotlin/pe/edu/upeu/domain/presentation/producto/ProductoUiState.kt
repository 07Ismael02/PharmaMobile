package pe.edu.upeu.domain.presentation.producto

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria

sealed interface Fase {
    data object Cargando : Fase
    data object SinProductos : Fase
    data object ConProductos : Fase
    data class Error(val detalle: String) : Fase
}

enum class TipoOperacion { Crear, Obtener, Actualizar, Eliminar }

sealed interface Operacion {
    data object Inactiva : Operacion
    data class EnCurso(val tipo: TipoOperacion, val productoId: Long? = null) : Operacion
    data class Fallida(val mensaje: String) : Operacion
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val categoriaId: Long? = null,
    val activo: Boolean = true,
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val errorCategoria: String? = null
)

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val productos: List<Producto> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val editandoId: Long? = null,
    val mensaje: String? = null,
    val tabSeleccionada: Int = 0
) {
    val guardando: Boolean
        get() = operacion is Operacion.EnCurso &&
            (operacion.tipo == TipoOperacion.Crear || operacion.tipo == TipoOperacion.Actualizar)
}
