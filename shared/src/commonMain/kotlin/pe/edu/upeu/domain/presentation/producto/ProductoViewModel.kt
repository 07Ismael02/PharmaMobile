package pe.edu.upeu.domain.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val repository: ProductoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun onNombreChange(valor: String) = actualizarFormulario {
        copy(nombre = valor, errorNombre = null)
    }

    fun onPrecioChange(valor: String) = actualizarFormulario {
        copy(precio = valor, errorPrecio = null)
    }

    fun onStockChange(valor: String) = actualizarFormulario {
        copy(stock = valor, errorStock = null)
    }

    fun onTabSeleccionada(indice: Int) {
        _uiState.update { it.copy(tabSeleccionada = indice) }
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = Fase.Cargando) }
            runCatching { repository.listar() }
                .onSuccess { productos ->
                    _uiState.update {
                        it.copy(
                            fase = if (productos.isEmpty()) Fase.SinProductos else Fase.ConProductos,
                            productos = productos
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(fase = Fase.Error(error.message ?: ERROR_CARGA))
                    }
                }
        }
    }

    fun registrar() {
        val formulario = _uiState.value.formulario
        if (_uiState.value.guardando) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    guardando = true,
                    mensaje = null,
                    formulario = formulario.copy(
                        errorNombre = null,
                        errorPrecio = null,
                        errorStock = null
                    )
                )
            }

            registrarProducto(formulario.nombre, formulario.precio, formulario.stock)
                .onSuccess { producto ->
                    val productos = repository.listar()
                    _uiState.update {
                        it.copy(
                            fase = if (productos.isEmpty()) Fase.SinProductos else Fase.ConProductos,
                            productos = productos,
                            formulario = FormularioProducto(),
                            guardando = false,
                            mensaje = "Producto \"${producto.nombre}\" registrado correctamente"
                        )
                    }
                }
                .onFailure { error ->
                    val detalle = error.message ?: ERROR_REGISTRO
                    _uiState.update {
                        it.copy(
                            guardando = false,
                            formulario = it.formulario.conError(detalle),
                            mensaje = if (detalle.esErrorValidacion()) null else detalle
                        )
                    }
                }
        }
    }

    private fun actualizarFormulario(transformar: FormularioProducto.() -> FormularioProducto) {
        _uiState.update {
            it.copy(formulario = it.formulario.transformar(), mensaje = null)
        }
    }

    private fun FormularioProducto.conError(detalle: String) = when (detalle) {
        RegistrarProductoUseCase.NOMBRE_ERROR -> copy(errorNombre = detalle)
        RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR,
        RegistrarProductoUseCase.PRECIO_RANGO_ERROR -> copy(errorPrecio = detalle)
        RegistrarProductoUseCase.STOCK_ENTERO_ERROR,
        RegistrarProductoUseCase.STOCK_RANGO_ERROR -> copy(errorStock = detalle)
        else -> this
    }

    private fun String.esErrorValidacion() = this in ERRORES_VALIDACION

    companion object {
        private const val ERROR_CARGA = "No se pudieron cargar los productos."
        private const val ERROR_REGISTRO = "No se pudo registrar el producto."
        private val ERRORES_VALIDACION = setOf(
            RegistrarProductoUseCase.NOMBRE_ERROR,
            RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR,
            RegistrarProductoUseCase.PRECIO_RANGO_ERROR,
            RegistrarProductoUseCase.STOCK_ENTERO_ERROR,
            RegistrarProductoUseCase.STOCK_RANGO_ERROR
        )
    }
}
