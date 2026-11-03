package pe.edu.upeu.domain.port

/** Contexto técnico únicamente; nunca incluir JWT, credenciales ni datos personales. */
interface Telemetria {
    fun registrarError(error: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}
