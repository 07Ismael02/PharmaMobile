package pe.edu.upeu.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.Logger
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.data.auth.AlmacenSeguro
import pe.edu.upeu.data.auth.AlmacenSeguroAndroid
import pe.edu.upeu.data.auth.VinculacionProtegida
import pe.edu.upeu.data.local.CreadorDeDriver
import pe.edu.upeu.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.data.remote.CreadorMotorHttp

actual val platformModule = module {
    single<CreadorDeDriver> { CreadorDeDriverAndroid(get<Context>()) }
    single { AlmacenSeguroAndroid(get<Context>()) }
    single<Compartidor> { CompartidorAndroid(get<Context>()) }
    single<AlmacenSeguro> { get<AlmacenSeguroAndroid>() }
    single<VinculacionProtegida> { get<AlmacenSeguroAndroid>() }
    single<CreadorMotorHttp> { object : CreadorMotorHttp { override fun crear(): HttpClientEngine = OkHttp.create() } }
    single<HttpClientEngine> { OkHttp.create() }
    single(named("urlBase")) { "http://10.0.2.2:8080/api/v1/" }
    single(named("esDepuracion")) {
        (get<Context>().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    single<Logger> {
        object : Logger {
            override fun log(message: String) {
                Log.d("PharmaMobilKtor", message)
            }
        }
    }
}
