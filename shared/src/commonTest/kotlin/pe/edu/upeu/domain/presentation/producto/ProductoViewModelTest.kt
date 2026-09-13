package pe.edu.upeu.domain.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase
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
            assertTrue(fase.detalle.contains("inventario", ignoreCase = true))
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
            viewModel.registrar()
            advanceUntilIdle()

            assertEquals(
                RegistrarProductoUseCase.PRECIO_RANGO_ERROR,
                viewModel.uiState.value.formulario.errorPrecio
            )
            assertEquals(0, repository.registrarCalls)
        }
    }

    private fun crearViewModel(repository: ProductoRepository) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repository),
        repository = repository
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

private class FakeProductoRepositoryVacio : ProductoRepository {
    override suspend fun listar(): List<Producto> = emptyList()
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryConProductos(
    private val productos: List<Producto>
) : ProductoRepository {
    override suspend fun listar(): List<Producto> = productos
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryError : ProductoRepository {
    override suspend fun listar(): List<Producto> = error("Error controlado al cargar inventario")
    override suspend fun registrar(producto: Producto): Producto = producto
}

private class FakeProductoRepositoryRegistroEspia : ProductoRepository {
    var registrarCalls: Int = 0
        private set

    override suspend fun listar(): List<Producto> = emptyList()

    override suspend fun registrar(producto: Producto): Producto {
        registrarCalls++
        return producto
    }
}
