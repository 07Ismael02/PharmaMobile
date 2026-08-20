package pe.edu.upeu.domain.model

data class DetallePedido(
    val id: Long,
    val producto: Producto,
    val cantidad: Int
){
    init {
        require(cantidad > 0){
            "La cantidad debe ser mayor que 0"
        }
    }
    fun subtotal(): Double {
        return producto.precio * cantidad
    }
}
