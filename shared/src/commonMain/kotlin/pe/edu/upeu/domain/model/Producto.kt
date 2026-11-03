package pe.edu.upeu.domain.model

enum class EstadoProductoLocal { SINCRONIZADO, PENDIENTE, FALLIDO, EN_REVISION }

data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true,
    val categoriaId: Long? = null,
    val estadoLocal: EstadoProductoLocal = EstadoProductoLocal.SINCRONIZADO,
    val ultimoError: String? = null,
) {
    val requiereReposicion: Boolean
        get() = stock <= STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 5
    }
}
