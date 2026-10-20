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

    /** Solo los registros nuevos que aún no iniciaron un POST. */
    fun pendientes(): List<Producto> = consultas.listarPendientesPorEnviar()
        .executeAsList()
        .map { it.aDominio() }

    /** Incluye los envíos ambiguos: nunca se borran ni reenvían por coincidencia de nombre. */
    fun pendientesEnRevision(): List<Producto> = consultas.listarPendientes()
        .executeAsList().filter { it.intentoEnvio == 1L }.map { it.aDominio() }

    fun listar(): List<Producto> = consultas.listarTodos()
        .executeAsList()
        .map { it.aDominio() }

    fun buscar(id: Long): Producto? = consultas.buscarPorId(id)
        .executeAsOneOrNull()
        ?.aDominio()

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
            pendiente = 1L,
            intentoEnvio = 0L
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
                    pendiente = 0L,
                    intentoEnvio = 1L
                )
            }
        }
    }

    fun confirmarPendiente(idLocal: Long, confirmado: Producto) {
        require(idLocal < 0 && confirmado.id > 0) { "La confirmación debe contener un ID real positivo" }
        consultas.transaction {
            check(consultas.buscarPorId(idLocal).executeAsOneOrNull()?.let {
                it.pendiente == 1L && it.intentoEnvio == 1L
            } == true) {
                "El producto local no tiene un envío confirmado pendiente"
            }
            consultas.borrarPendiente(idLocal)
            insertarSincronizado(confirmado)
        }
    }

    /** La marca se confirma en SQLite antes del POST; si se pierde la respuesta no se reenvía. */
    fun marcarIntentoEnvio(idLocal: Long) = consultas.transaction {
        check(consultas.buscarPorId(idLocal).executeAsOneOrNull()?.let {
            it.pendiente == 1L && it.intentoEnvio == 0L
        } == true) { "El pendiente ya fue enviado o requiere revisión" }
        consultas.marcarIntentoEnvio(idLocal)
        check(consultas.buscarPorId(idLocal).executeAsOneOrNull()?.let {
            it.pendiente == 1L && it.intentoEnvio == 1L
        } == true) { "El pendiente ya no está disponible para enviar" }
    }

    /** Solo para rechazos de seguridad (401/403), que ocurren antes de crear el recurso. */
    fun revertirIntentoEnvio(idLocal: Long) = consultas.revertirIntentoEnvio(idLocal)

    fun guardarSincronizado(producto: Producto) {
        require(producto.id > 0) { "El producto del servidor debe tener ID positivo" }
        insertarSincronizado(producto)
    }

    fun marcarInactivo(id: Long) {
        require(id > 0) { "La baja requiere un ID del servidor" }
        consultas.marcarInactivo(id)
    }

    private fun insertarSincronizado(producto: Producto) {
        consultas.insertar(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            activo = if (producto.activo) 1L else 0L,
            categoriaId = producto.categoriaId,
            pendiente = 0L,
            intentoEnvio = 1L
        )
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
