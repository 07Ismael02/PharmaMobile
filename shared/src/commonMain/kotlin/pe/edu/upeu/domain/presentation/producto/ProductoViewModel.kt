package pe.edu.upeu.domain.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.domain.error.ErrorApi
import pe.edu.upeu.domain.error.ErrorApiException
import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarCategoriasUseCase
import pe.edu.upeu.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val listarProductos: ListarProductosUseCase,
    private val obtenerProducto: ObtenerProductoUseCase,
    private val registrarProducto: RegistrarProductoUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
    private val listarCategorias: ListarCategoriasUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
        cargarCategorias()
    }

    fun onNombreChange(valor: String) = actualizarFormulario { copy(nombre = valor, errorNombre = null) }
    fun onPrecioChange(valor: String) = actualizarFormulario { copy(precio = valor, errorPrecio = null) }
    fun onStockChange(valor: String) = actualizarFormulario { copy(stock = valor, errorStock = null) }
    fun onCategoriaSeleccionada(id: Long) = actualizarFormulario { copy(categoriaId = id, errorCategoria = null) }

    fun onTabSeleccionada(indice: Int) {
        _uiState.update { it.copy(tabSeleccionada = indice) }
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = Fase.Cargando) }
            listarProductos().fold(
                onSuccess = { productos -> mostrarProductos(productos) },
                onFailure = { fallo -> _uiState.update { it.copy(fase = Fase.Error(mensajeDe(fallo))) } }
            )
        }
    }

    fun cargarCategorias() {
        viewModelScope.launch {
            listarCategorias().fold(
                onSuccess = { categorias -> _uiState.update { it.copy(categorias = categorias.filter { c -> c.activa }) } },
                onFailure = { fallo -> _uiState.update { it.copy(operacion = Operacion.Fallida("No se pudieron cargar las categorías. ${mensajeDe(fallo)}")) } }
            )
        }
    }

    fun registrar() {
        val estado = _uiState.value
        if (estado.operacion is Operacion.EnCurso) return
        val formulario = estado.formulario
        if (formulario.categoriaId == null) {
            _uiState.update { it.copy(formulario = it.formulario.copy(errorCategoria = "Seleccione una categoría.")) }
            return
        }
        viewModelScope.launch {
            iniciarOperacion(TipoOperacion.Crear)
            registrarProducto(formulario.nombre, formulario.precio, formulario.stock, formulario.categoriaId).fold(
                onSuccess = { producto -> refrescarTrasMutacion("Producto \"${producto.nombre}\" registrado correctamente") },
                onFailure = ::manejarFallo
            )
        }
    }

    /** Consulta GET por ID antes de cargar el formulario de edición. */
    fun editar(id: Long) {
        if (_uiState.value.operacion is Operacion.EnCurso) return
        viewModelScope.launch {
            iniciarOperacion(TipoOperacion.Obtener, id)
            obtenerProducto(id).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            editandoId = id,
                            formulario = FormularioProducto(
                                nombre = producto.nombre,
                                precio = producto.precio.toString(),
                                stock = producto.stock.toString(),
                                categoriaId = producto.categoriaId,
                                activo = producto.activo
                            ),
                            operacion = Operacion.Inactiva
                        )
                    }
                },
                onFailure = ::manejarFallo
            )
        }
    }

    fun cancelarEdicion() {
        _uiState.update { it.copy(editandoId = null, formulario = FormularioProducto(), mensaje = null, mensajeExito = null) }
    }

    fun actualizar() {
        val estado = _uiState.value
        val id = estado.editandoId ?: return
        if (estado.operacion is Operacion.EnCurso) return
        val formulario = estado.formulario
        if (!validarFormulario(formulario)) return
        viewModelScope.launch {
            iniciarOperacion(TipoOperacion.Actualizar, id)
            val producto = Producto(
                id = id,
                nombre = formulario.nombre.trim(),
                precio = requireNotNull(formulario.precio.toDoubleOrNull()),
                stock = requireNotNull(formulario.stock.toIntOrNull()),
                activo = formulario.activo,
                categoriaId = formulario.categoriaId
            )
            actualizarProducto(producto).fold(
                onSuccess = { actualizado -> refrescarTrasMutacion("Producto \"${actualizado.nombre}\" actualizado correctamente") },
                onFailure = ::manejarFallo
            )
        }
    }

    fun eliminar(id: Long) {
        if (_uiState.value.operacion is Operacion.EnCurso) return
        viewModelScope.launch {
            iniciarOperacion(TipoOperacion.Eliminar, id)
            eliminarProducto(id).fold(
                onSuccess = { refrescarTrasMutacion("Producto dado de baja correctamente") },
                onFailure = ::manejarFallo
            )
        }
    }

    private suspend fun refrescarTrasMutacion(mensaje: String) {
        listarProductos().fold(
            onSuccess = { productos ->
                _uiState.update {
                    it.copy(
                        fase = productos.toFase(),
                        formulario = FormularioProducto(categoriaId = it.formulario.categoriaId),
                        editandoId = null,
                        operacion = Operacion.Inactiva,
                        mensajeExito = mensaje
                    )
                }
            },
            onFailure = { fallo ->
                _uiState.update {
                    it.copy(operacion = Operacion.Fallida("La operación se completó, pero no se pudo refrescar la lista: ${mensajeDe(fallo)}"))
                }
            }
        )
    }

    private fun mostrarProductos(productos: List<Producto>) {
        _uiState.update {
            it.copy(
                fase = productos.toFase()
            )
        }
    }

    private fun List<Producto>.toFase(): Fase =
        if (isEmpty()) Fase.SinProductos else Fase.ConProductos(map { it.toUi() })

    private fun iniciarOperacion(tipo: TipoOperacion, id: Long? = null) {
        _uiState.update {
            it.copy(
                operacion = Operacion.EnCurso(tipo, id),
                mensaje = null,
                mensajeExito = null,
                formulario = it.formulario.copy(
                    errorNombre = null, errorPrecio = null, errorStock = null, errorCategoria = null
                )
            )
        }
    }

    private fun validarFormulario(formulario: FormularioProducto): Boolean {
        val nombre = RegistrarProductoUseCase.validarNombre(formulario.nombre)
        val precio = RegistrarProductoUseCase.validarPrecio(formulario.precio)
        val stock = RegistrarProductoUseCase.validarStock(formulario.stock)
        val categoria = if (formulario.categoriaId == null) "Seleccione una categoría." else null
        _uiState.update {
            it.copy(formulario = it.formulario.copy(
                errorNombre = nombre, errorPrecio = precio, errorStock = stock, errorCategoria = categoria
            ))
        }
        return nombre == null && precio == null && stock == null && categoria == null
    }

    private fun manejarFallo(fallo: Throwable) {
        val error = (fallo as? ErrorApiException)?.error
        if (error is ErrorApi.Validacion) {
            _uiState.update {
                it.copy(
                    operacion = Operacion.Inactiva,
                    formulario = it.formulario.copy(
                        errorNombre = error.porCampo["nombre"],
                        errorPrecio = error.porCampo["precio"],
                        errorStock = error.porCampo["stock"],
                        errorCategoria = error.porCampo["categoriaId"]
                    ),
                    mensaje = if (error.porCampo.isEmpty()) "Revise los datos del formulario." else null
                )
            }
            return
        }
        val detalle = when (fallo.message) {
            RegistrarProductoUseCase.NOMBRE_ERROR,
            RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR,
            RegistrarProductoUseCase.PRECIO_RANGO_ERROR,
            RegistrarProductoUseCase.STOCK_ENTERO_ERROR,
            RegistrarProductoUseCase.STOCK_RANGO_ERROR -> fallo.message
            else -> null
        }
        if (detalle != null) {
            _uiState.update { it.copy(operacion = Operacion.Inactiva, formulario = it.formulario.conError(detalle)) }
        } else {
            _uiState.update { it.copy(operacion = Operacion.Fallida(mensajeDe(fallo))) }
        }
    }

    private fun actualizarFormulario(transformar: FormularioProducto.() -> FormularioProducto) {
        _uiState.update { it.copy(formulario = it.formulario.transformar(), mensaje = null, mensajeExito = null) }
    }

    private fun FormularioProducto.conError(detalle: String) = when (detalle) {
        RegistrarProductoUseCase.NOMBRE_ERROR -> copy(errorNombre = detalle)
        RegistrarProductoUseCase.PRECIO_NUMERICO_ERROR,
        RegistrarProductoUseCase.PRECIO_RANGO_ERROR -> copy(errorPrecio = detalle)
        RegistrarProductoUseCase.STOCK_ENTERO_ERROR,
        RegistrarProductoUseCase.STOCK_RANGO_ERROR -> copy(errorStock = detalle)
        else -> this
    }

    private fun mensajeDe(fallo: Throwable): String = when (val error = (fallo as? ErrorApiException)?.error) {
        ErrorApi.NoEncontrado -> "El producto ya no existe. Actualice la lista."
        is ErrorApi.Conflicto -> error.mensaje
        ErrorApi.Servidor -> "El servidor no pudo completar la operación."
        ErrorApi.SinConexion -> "Sin conexión con el backend. Reintente."
        ErrorApi.TiempoAgotado -> "La solicitud tardó demasiado. Reintente."
        is ErrorApi.Validacion -> "Revise los datos del formulario."
        null -> "No se pudo completar la operación."
    }
}
