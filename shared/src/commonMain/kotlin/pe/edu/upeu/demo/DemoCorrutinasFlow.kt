package pe.edu.upeu.demo

import kotlinx.coroutines.runBlocking
import pe.edu.upeu.data.repository.observarEstados
import pe.edu.upeu.data.repository.observarProductos
import pe.edu.upeu.data.repository.cargarProductos
import pe.edu.upeu.domain.model.ResultadoProductos

fun probarCorrutinasYFlows() = runBlocking {
    println("--- 1. PROBANDO FLUJO DE ESTADOS (STRINGS) ---")
    observarEstados().collect { mensaje ->
        println("Mensaje recibido: $mensaje")
    }

    println("\n--- 2. PROBANDO FLUJO DE PRODUCTOS (.COPY) ---")
    observarProductos().collect { lista ->
        println("Lista recibida con ${lista.size} productos.")
        lista.forEach { println(" - ${it.nombre} | Stock: ${it.stock}") }
    }

    println("\n--- 3. PROBANDO FLUJO INTEGRADO (SEALED CLASS) ---")
    cargarProductos().collect { resultado ->
        when (resultado) {
            is ResultadoProductos.Cargando -> println("UI: Cargando productos...")
            is ResultadoProductos.Exito -> println("UI Éxito: Se obtuvieron ${resultado.productos.size} productos correctamente.")
            is ResultadoProductos.Error -> println("UI Error: ${resultado.mensaje}")
        }
    }
}