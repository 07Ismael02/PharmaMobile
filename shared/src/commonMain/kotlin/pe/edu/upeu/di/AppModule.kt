package pe.edu.upeu.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.data.remote.crearHttpClient
import pe.edu.upeu.data.remote.ProductoApi
import pe.edu.upeu.data.remote.CategoriaApi
import pe.edu.upeu.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.data.repository.ProductoRepositorioRest
import pe.edu.upeu.domain.presentation.producto.ProductoViewModel
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarCategoriasUseCase

val dataModule = module {
    single<HttpClient> { crearHttpClient(get(), get(named("urlBase")), get()) }
    single { ProductoApi(get()) }
    single { CategoriaApi(get()) }
    single { ProductoRepositorioEnMemoria() }
    single<ProductoRepository> { ProductoRepositorioRest(get(), get()) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { ListarCategoriasUseCase(get()) }
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
