package pe.edu.upeu.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Siempre se construye con la PharmaDb propiedad del ámbito de cuenta vigente. */
class CategoriaLocal(base: PharmaDb) {
    private val consultas = base.categoriaQueries

    fun observar(): Flow<List<Categoria>> = consultas.listarCategorias()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { filas -> filas.map { Categoria(it.id, it.nombre, it.activa == 1L) } }

    fun listar(): List<Categoria> = consultas.listarCategorias().executeAsList()
        .map { Categoria(it.id, it.nombre, it.activa == 1L) }

    fun reemplazarSincronizadas(categorias: List<Categoria>) {
        require(categorias.all { it.id > 0 }) { "Las categorías remotas deben tener ID positivo" }
        consultas.transaction {
            consultas.borrarCategorias()
            categorias.forEach {
                consultas.insertarCategoria(it.id, it.nombre, if (it.activa) 1L else 0L)
            }
        }
    }
}
