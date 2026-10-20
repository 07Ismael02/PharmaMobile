package pe.edu.upeu.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

class CreadorDeDriverAndroid(private val contexto: Context) : CreadorDeDriver {
    override fun crear(nombreArchivo: String): SqlDriver = AndroidSqliteDriver(
        schema = PharmaDb.Schema,
        context = contexto,
        name = nombreArchivo.also { require(it.matches(Regex("pharma-cuenta-[0-9a-f]{32}\\.db"))) }
    )
}
