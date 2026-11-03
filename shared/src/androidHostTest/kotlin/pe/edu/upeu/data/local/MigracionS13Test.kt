package pe.edu.upeu.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals

class MigracionS13Test {
    @Test
    fun migrarDesdeVersionCuatroConservaPendienteAmbiguo() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            driver.execute(null, """CREATE TABLE producto (
                id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, precio REAL NOT NULL,
                stock INTEGER NOT NULL, activo INTEGER NOT NULL, categoriaId INTEGER,
                pendiente INTEGER NOT NULL DEFAULT 0, intentoEnvio INTEGER NOT NULL DEFAULT 1
            )""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente, intentoEnvio)
                VALUES (-1, 'Pendiente anterior', 4.5, 7, 1, 1, 1, 1)""", 0)
            PharmaDb.Schema.migrate(driver, 4, 5)
            val local = ProductoLocal(PharmaDb(driver))
            assertEquals(-1L, local.listar().single().id)
            assertEquals(0, local.intentos(-1))
            assertEquals(1, runBlocking { local.observarFallidas().first() })
        }
    }
}
