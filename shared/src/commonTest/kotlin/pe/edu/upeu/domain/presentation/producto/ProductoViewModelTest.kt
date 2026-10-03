package pe.edu.upeu.domain.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.model.Categoria
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarCategoriasUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @Test
    fun repositorioVacioProduceFaseSinProductos() = runTest {
        probarConMainDeTest {
            val viewModel = crearViewModel(FakeProductoRepositoryVacio())

            advanceUntilIdle()

            assertIs<Fase.SinProductos>(viewModel.uiState.value.fase)
            assertTrue(viewModel.uiState.value.productos.isEmpty())
        }
    }

    @Test
    fun repositorioConTresProductosProduceFaseConProductosYExponeLaLista() = runTest {
        probarConMainDeTest {
            val productosEsperados = listOf(
                Producto(1L, "Paracetamol", 15.50, 100),
                Producto(2L, "Loratadina", 12.50, 0, activo = false),
                Producto(3L, "Diclofenaco", 20.00, 3)
            )
            val viewModel = crearViewModel(FakeProductoRepositoryConProductos(productosEsperados))

            advanceUntilIdle()

            assertIs<Fase.ConProductos>(viewModel.uiState.value.fase)
            assertEquals(3, viewModel.uiState.value.productos.size)
            assertEquals(productosEsperados, viewModel.uiState.value.productos)
        }
    }

    @Test
    fun errorAlListarProduceFaseErrorConDetalleCoherente() = runTest {
        probarConMainDeTest {
            val viewModel = crearViewModel(FakeProductoRepositoryError())

            advanceUntilIdle()

            val fase = assertIs<Fase.Error>(viewModel.uiState.value.fase)
            assertTrue(fase.detalle.contains("No se pudo completar", ignoreCase = true))
        }
    }

    @Test
    fun precioCeroMuestraErrorYNoInvocaRegistrar() = runTest {
        probarConMainDeTest {
            val repository = FakeProductoRepositoryRegistroEspia()
            val viewModel = crearViewModel(repository)
            advanceUntilIdle()

            viewModel.onNombreChange("Producto válido")
            viewModel.onPrecioChange("0")
            viewModel.onStockChange("0")
            viewModel.onCategoriaSeleccionada(1)
            viewModel.registrar()
            advanceUntilIdle()

            assertEquals(
                RegistrarProductoUseCase.PRECIO_RANGO_ERROR,
                viewModel.uiState.value.formulario.errorPrecio
            )
            assertEquals(0, repository.registrarCalls)
        }
    }

    @Test
    fun crearRecargaListaYConservaFaseVisible() = runTest {
        probarConMainDeTest {
            val repository = FakeProductoRepositoryMutable()
            val viewModel = crearViewModel(repository)
            advanceUntilIdle()
            viewModel.onNombreChange("Producto prueba")
            viewModel.onPrecioChange("4.5")
            viewModel.onStockChange("8")
            viewModel.onCategoriaSeleccionada(1)
            viewModel.registrar()
            advanceUntilIdle()

            assertIs<Fase.ConProductos>(viewModel.uiState.value.fase)
            assertIs<Operacion.Inactiva>(viewModel.uiState.value.operacion)
            assertEquals(1, viewModel.uiState.value.productos.size)
            assertTrue(repository.listados >= 2)
        }
    }

    @Test
    fun validacionServidorSeMuestraBajoCampoSinOcultarLista() = runTest {
        probarConMainDeTest {
            val repository = FakeProductoRepositoryMutable()
            repository.falloRegistro = ErrorApiException(
                ErrorApi.Validacion(mapOf("nombre" to "El nombre debe tener entre 3 y 150 caracteres"))
            )
            val viewModel = crearViewModel(repository)
            advanceUntilIdle()
            viewModel.onNombreChange("AB")
            viewModel.onPrecioChange("4.5")
            viewModel.onStockChange("8")
            viewModel.onCategoriaSeleccionada(1)
            viewModel.registrar()
            advanceUntilIdle()

            assertIs<Fase.SinProductos>(viewModel.uiState.value.fase)
            assertEquals("El nombre debe tener entre 3 y 150 caracteres", viewModel.uiState.value.formulario.errorNombre)
            assertIs<Operacion.Inactiva>(viewModel.uiState.value.operacion)
        }
    }

    @Test
    fun eliminarMantieneListaMientrasOperaYRecargaEstadoInactivo() = runTest {
        probarConMainDeTest {
            val repository = FakeProductoRepositoryMutable()
            repository.registrar(Producto(1, "Producto prueba", 4.5, 8, categoriaId = 1))
            val barrera = CompletableDeferred<Unit>()
            repository.esperaEliminacion = barrera
            val viewModel = crearViewModel(repository)
            advanceUntilIdle()

            viewModel.eliminar(1)
            runCurrent()
            assertIs<Fase.ConProductos>(viewModel.uiState.value.fase)
            assertIs<Operacion.EnCurso>(viewModel.uiState.value.operacion)

            barrera.complete(Unit)
            advanceUntilIdle()
            assertIs<Operacion.Inactiva>(viewModel.uiState.value.operacion)
            assertEquals(false, viewModel.uiState.value.productos.single().activo)
            assertTrue(repository.listados >= 2)
        }
    }

    private fun crearViewModel(repository: ProductoRepository) = ProductoViewModel(
        listarProductos = ListarProductosUseCase(repository),
        obtenerProducto = ObtenerProductoUseCase(repository),
        registrarProducto = RegistrarProductoUseCase(repository),
        actualizarProducto = ActualizarProductoUseCase(repository),
        eliminarProducto = EliminarProductoUseCase(repository),
        listarCategorias = ListarCategoriasUseCase(repository)
    )

    private suspend fun kotlinx.coroutines.test.TestScope.probarConMainDeTest(
        bloque: suspend kotlinx.coroutines.test.TestScope.() -> Unit
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            bloque()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private abstract class BaseFakeProductoRepository : ProductoRepository {
    override suspend fun obtener(id: Long): Producto = listar().first { it.id == id }
    override suspend fun actualizar(producto: Producto): Producto = producto
    override suspend fun eliminar(id: Long) = Unit
    override suspend fun listarCategorias(): List<Categoria> = listOf(Categoria(1, "Analgésicos", true))
}

private class FakeProductoRepositoryVacio : BaseFakeProductoRepository() {
    override suspend fun listar(): List<Producto> = emptyList()
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryConProductos(
    private val productos: List<Producto>
) : BaseFakeProductoRepository() {
    override suspend fun listar(): List<Producto> = productos
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryError : BaseFakeProductoRepository() {
    override suspend fun listar(): List<Producto> = error("Error controlado al cargar inventario")
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryRegistroEspia : BaseFakeProductoRepository() {
    var registrarCalls: Int = 0
        private set

    override suspend fun listar(): List<Producto> = emptyList()

    override suspend fun registrar(producto: Producto): Producto {
        registrarCalls++
        return producto
    }
}

private class FakeProductoRepositoryMutable : BaseFakeProductoRepository() {
    private val productos = mutableListOf<Producto>()
    var listados = 0
    var falloRegistro: Throwable? = null
    var esperaEliminacion: CompletableDeferred<Unit>? = null

    override suspend fun listar(): List<Producto> {
        listados++
        return productos.toList()
    }

    override suspend fun registrar(producto: Producto): Producto {
        falloRegistro?.let { throw it }
        val creado = producto.copy(id = (productos.maxOfOrNull { it.id } ?: 0L) + 1)
        productos += creado
        return creado
    }

    override suspend fun eliminar(id: Long) {
        esperaEliminacion?.await()
        val indice = productos.indexOfFirst { it.id == id }
        productos[indice] = productos[indice].copy(activo = false)
    }
}
