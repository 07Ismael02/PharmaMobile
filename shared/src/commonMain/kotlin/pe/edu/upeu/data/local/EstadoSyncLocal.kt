package pe.edu.upeu.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Marca temporal persistida exclusivamente en la base del ámbito de cuenta. */
class EstadoSyncLocal(base: PharmaDb) {
    private val consultas = base.estadoSyncQueries

    fun observarUltimaSincronizacion(): Flow<String?> = consultas.ultimaSincronizacion()
        .asFlow()
        .mapToOneOrNull(Dispatchers.Default)

    fun ultimaSincronizacion(): String? = consultas.ultimaSincronizacion()
        .executeAsOneOrNull()

    fun registrarExito() = consultas.registrarSincronizacion()
}
