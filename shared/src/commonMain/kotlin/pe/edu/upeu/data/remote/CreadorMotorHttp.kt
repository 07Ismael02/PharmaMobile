package pe.edu.upeu.data.remote

import io.ktor.client.engine.HttpClientEngine

/** Cada ámbito de cuenta posee un motor que puede cerrar sin afectar al login global. */
interface CreadorMotorHttp {
    fun crear(): HttpClientEngine
}
