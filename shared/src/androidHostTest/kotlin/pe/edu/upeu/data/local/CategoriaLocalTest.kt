package pe.edu.upeu.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoriaLocalTest {
    private fun baseNueva(): JdbcSqliteDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        PharmaDb.Schema.create(it)
    }

    @Test
    fun persisteYObservaCategorias() = runBlocking {
        baseNueva().use { driver ->
            val local = CategoriaLocal(PharmaDb(driver))
            local.reemplazarSincronizadas(listOf(Categoria(1, "Analgésicos", true)))
            assertEquals(listOf(Categoria(1, "Analgésicos", true)), local.listar())
            assertEquals(local.listar(), local.observar().first())
        }
    }

    @Test
    fun reemplazoNoBorraProductosPendientes() {
        baseNueva().use { driver ->
            val base = PharmaDb(driver)
            val pendiente = ProductoLocal(base).guardarPendiente(
                Producto(0, "Pendiente", 4.5, 7, true, 1)
            )
            val categorias = CategoriaLocal(base)
            categorias.reemplazarSincronizadas(listOf(Categoria(1, "Anterior", true)))
            categorias.reemplazarSincronizadas(listOf(Categoria(2, "Nueva", false)))
            assertEquals(listOf(Categoria(2, "Nueva", false)), categorias.listar())
            assertEquals(pendiente, ProductoLocal(base).pendientes().single())
        }
    }

    @Test
    fun basesDeAmbitosDistintosNoCompartenCategorias() {
        baseNueva().use { driverA ->
            baseNueva().use { driverB ->
                CategoriaLocal(PharmaDb(driverA)).reemplazarSincronizadas(
                    listOf(Categoria(1, "Cuenta A", true))
                )
                assertTrue(CategoriaLocal(PharmaDb(driverB)).listar().isEmpty())
            }
        }
    }

    @Test
    fun migracionDesdeVersionUnoConservaProductosYPendientes() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            driver.execute(null, """CREATE TABLE producto (
                id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, precio REAL NOT NULL,
                stock INTEGER NOT NULL, activo INTEGER NOT NULL DEFAULT 1,
                categoriaId INTEGER, pendiente INTEGER NOT NULL DEFAULT 0
            )""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente)
                VALUES (-1, 'Pendiente previo', 4.5, 7, 1, 1, 1)""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente)
                VALUES (-2, 'Segundo pendiente', 4.5, 7, 1, 1, 1)""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente)
                VALUES (-3, 'Tercer pendiente', 4.5, 7, 1, 1, 1)""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente)
                VALUES (41, 'Confirmado', 4.5, 7, 1, 1, 0)""", 0)
            driver.execute(null, """INSERT INTO producto
                (id, nombre, precio, stock, activo, categoriaId, pendiente)
                VALUES (42, 'Confirmado dos', 4.5, 7, 1, 1, 0)""", 0)
            assertEquals(4L, PharmaDb.Schema.version)
            PharmaDb.Schema.migrate(driver, 1, PharmaDb.Schema.version)
            val base = PharmaDb(driver)
            assertEquals(setOf(-1L, -2L, -3L),
                ProductoLocal(base).pendientesEnRevision().map { it.id }.toSet())
            assertTrue(ProductoLocal(base).pendientes().isEmpty())
            assertEquals(setOf(-1L, -2L, -3L, 41L, 42L),
                ProductoLocal(base).listar().map { it.id }.toSet())
            assertTrue(CategoriaLocal(base).listar().isEmpty())
            CategoriaLocal(base).reemplazarSincronizadas(listOf(Categoria(1, "Analgésicos", true)))
            assertFalse(ProductoLocal(base).pendientesEnRevision().isEmpty())
        }
    }
}
