package pe.edu.upeu.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.data.repository.ProductoRepositorioRest
import pe.edu.upeu.domain.presentation.producto.ProductoViewModel
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase

val dataModule = module {
    single<HttpClient> { crearHttpClient(get(), get(named("urlBase"))) }
    single { ProductoApi(get()) }
    single { ProductoRepositorioEnMemoria() }
    single<ProductoRepository> { ProductoRepositorioRest(get(), get()) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::ProductoViewModel)
}

expect val platformModule: Module

fun initKoin() {
    startKoin {
        modules(dataModule, domainModule, presentationModule, platformModule)
    }
}
