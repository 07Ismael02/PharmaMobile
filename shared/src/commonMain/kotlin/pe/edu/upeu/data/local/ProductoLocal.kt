package pe.edu.upeu.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as ProductoFila

class ProductoLocal(base: PharmaDb) {
    private val consultas = base.productoQueries

    fun observar(): Flow<List<Producto>> = consultas.listarTodos()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { filas -> filas.map { it.aDominio() } }

    fun pendientes(): List<Producto> = consultas.listarPendientes()
        .executeAsList()
        .map { it.aDominio() }

    fun guardarPendiente(producto: Producto): Producto = consultas.transactionWithResult {
        val idLocal = consultas.siguienteIdLocal().executeAsOne()
        check(idLocal < 0) { "Se agotaron los identificadores locales negativos" }
        val local = producto.copy(id = idLocal)
        consultas.insertar(
            id = local.id,
            nombre = local.nombre,
            precio = local.precio,
            stock = local.stock.toLong(),
            activo = if (local.activo) 1L else 0L,
            categoriaId = local.categoriaId,
            pendiente = 1L
        )
        local
    }

    fun reemplazarSincronizados(productos: List<Producto>) {
        require(productos.all { it.id > 0 }) { "Los productos del servidor deben tener ID positivo" }
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach { producto ->
                consultas.insertar(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precio,
                    stock = producto.stock.toLong(),
                    activo = if (producto.activo) 1L else 0L,
                    categoriaId = producto.categoriaId,
                    pendiente = 0L
                )
            }
        }
    }
}

private fun ProductoFila.aDominio(): Producto {
    check(stock in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
        "El stock local excede el rango admitido por Producto"
    }
    return Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        activo = activo == 1L,
        categoriaId = categoriaId
    )
}
