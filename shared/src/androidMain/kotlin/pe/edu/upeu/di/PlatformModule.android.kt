package pe.edu.upeu.di

import android.content.Context
import android.util.Log
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logger
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule = module {
    single<Compartidor> { CompartidorAndroid(get<Context>()) }
    single<HttpClientEngine> { OkHttp.create() }
    single(named("urlBase")) { "http://10.0.2.2:8080/api/v1/" }
    single<Logger> {
        object : Logger {
            override fun log(message: String) {
                Log.d("PharmaMobilKtor", message)
            }
        }
    }
}
