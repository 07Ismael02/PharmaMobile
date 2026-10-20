package pe.edu.upeu.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

class CreadorDeDriverIos : CreadorDeDriver {
    override fun crear(nombreArchivo: String): SqlDriver = NativeSqliteDriver(
        schema = PharmaDb.Schema,
        name = nombreArchivo.also { require(it.matches(Regex("pharma-cuenta-[0-9a-f]{32}\\.db"))) }
    )
}
