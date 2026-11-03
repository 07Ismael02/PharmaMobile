package pe.edu.upeu.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.domain.port.Telemetria

/** Solo se registra en Koin cuando existe configuración Firebase real. */
class TelemetriaFirebase : Telemetria {
    override fun registrarError(error: Throwable, contexto: Map<String, String>) {
        // El mensaje original puede contener URL, token o datos del servidor.
        val clavesPermitidas = setOf("pantalla", "operacion", "codigo")
        val seguro = contexto.filterKeys { it in clavesPermitidas }
            .mapValues { (_, valor) -> valor.take(40).replace(Regex("[^A-Za-z0-9_-]"), "_") }
        Firebase.crashlytics.recordException(
            IllegalStateException(error::class.simpleName ?: "ErrorTecnico"), seguro
        )
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        Firebase.crashlytics.log("evento:$nombre")
    }
}

/** Mantiene REST/CRUD operativo mientras se configura el proyecto externo. */
class TelemetriaPendiente : Telemetria {
    override fun registrarError(error: Throwable, contexto: Map<String, String>) = Unit
    override fun registrarEvento(nombre: String, datos: Map<String, String>) = Unit
}
