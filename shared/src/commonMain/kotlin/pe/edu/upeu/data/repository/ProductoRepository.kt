package pe.edu.upeu.data.repository

import pe.edu.upeu.domain.model.Producto
import pe.edu.upeu.domain.datasource.productosSimulados
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.domain.model.ResultadoProductos

fun observarEstados(): Flow<String> = flow {
    emit("Iniciando carga de inventario...")
    delay(1000)
    emit("Procesando productos de PharmaMobile...")
    delay(1000)
    emit("Carga finalizada con éxito")
}

fun observarProductos(): Flow<List<Producto>> = flow {
    emit(emptyList()) // 1. Emisión inicial vacía
    delay(1000)

    emit(productosSimulados) // 2. Emisión con los productos iniciales
    delay(1000)

    // 3. Simulación de actualización dinámica del stock usando .copy()
    val productosActualizados = productosSimulados.mapIndexed { index, producto ->
        if (index == 0) producto.copy(stock = producto.stock - 5) else producto
    }
    emit(productosActualizados)
}

fun cargarProductos(): Flow<ResultadoProductos> = flow {
    emit(ResultadoProductos.Cargando)
    delay(1000)
    try {
        emit(ResultadoProductos.Exito(productosSimulados))
    } catch (e: Exception) {
        emit(ResultadoProductos.Error("Error al conectar con el servidor: ${e.message}"))
    }
}