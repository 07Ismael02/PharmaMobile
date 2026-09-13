package pe.edu.upeu.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.upeu.domain.model.Producto

class ProductoRepositorioEnMemoriaTest {
    @Test
    fun repositorioAsignaIdYPersisteProducto() = runBlocking {
        val repository = ProductoRepositorioEnMemoria()
        val registradosAntes = repository.listar()

        val registrado = repository.registrar(
            Producto(id = 0L, nombre = "Producto de prueba", precio = 10.0, stock = 0)
        )
        val registradosDespues = repository.listar()

        assertEquals((registradosAntes.maxOfOrNull { it.id } ?: 0L) + 1L, registrado.id)
        assertTrue(registradosDespues.contains(registrado))
    }
}
