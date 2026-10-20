package pe.edu.upeu.data.local

import app.cash.sqldelight.db.SqlDriver

interface CreadorDeDriver {
    /** Nunca recibe el nombre de la base histórica en cuarentena. */
    fun crear(nombreArchivo: String): SqlDriver
}
