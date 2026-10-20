package pe.edu.upeu.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import pe.edu.upeu.data.auth.AlmacenSeguro
import pe.edu.upeu.data.auth.AlmacenSeguroIos
import pe.edu.upeu.data.auth.VinculacionProtegida
import pe.edu.upeu.data.local.CreadorDeDriver
import pe.edu.upeu.data.local.CreadorDeDriverIos
import pe.edu.upeu.data.remote.CreadorMotorHttp

actual val platformModule = module {
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single { AlmacenSeguroIos() }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { get<AlmacenSeguroIos>() }
    single<VinculacionProtegida> { get<AlmacenSeguroIos>() }
    single<CreadorMotorHttp> { object : CreadorMotorHttp { override fun crear(): HttpClientEngine = Darwin.create() } }
    single<HttpClientEngine> { Darwin.create() }
    single(named("urlBase")) { "http://localhost:8080/api/v1/" }
    single(named("esDepuracion")) { false }
    single<Logger> { Logger.SIMPLE }
}
