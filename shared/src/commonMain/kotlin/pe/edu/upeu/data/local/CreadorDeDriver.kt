package pe.edu.upeu.data.local

import app.cash.sqldelight.db.SqlDriver

interface CreadorDeDriver {
    fun crear(): SqlDriver
}
