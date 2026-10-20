package pe.edu.upeu.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.random.Random

actual fun crearDriverPrueba(): SqlDriver = NativeSqliteDriver(
    schema = PharmaDb.Schema,
    name = "prueba-${Random.nextLong()}.db",
    onConfiguration = { it.copy(inMemory = true) }
)
