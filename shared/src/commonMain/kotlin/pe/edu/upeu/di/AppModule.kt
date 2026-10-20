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
import pe.edu.upeu.data.repository.ProductoRepositorioNoDisponible
import pe.edu.upeu.domain.presentation.producto.ProductoViewModel
import pe.edu.upeu.domain.repository.ProductoRepository
import pe.edu.upeu.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.domain.usecase.ListarCategoriasUseCase
import pe.edu.upeu.data.auth.AlmacenSeguro
import pe.edu.upeu.data.auth.AuthRepositorioRest
import pe.edu.upeu.domain.auth.AuthRepository
import pe.edu.upeu.domain.auth.CoordinadorSesion
import pe.edu.upeu.domain.auth.IniciarSesionUseCase
import pe.edu.upeu.domain.auth.RestaurarSesionUseCase
import pe.edu.upeu.domain.auth.CerrarSesionUseCase
import pe.edu.upeu.domain.presentation.auth.SesionViewModel

val dataModule = module {
    single { CoordinadorSesion() }
    single<HttpClient> {
        crearHttpClient(get(), get(named("urlBase")), get(), get<AlmacenSeguro>(), get(), get(named("esDepuracion")))
    }
    single<AuthRepository> { AuthRepositorioRest(get(), get(), get()) }
    single { ProductoApi(get()) }
    single { CategoriaApi(get()) }
    single { ProductoRepositorioEnMemoria() }
    single { ProductoRepositorioRest(get(), get()) }
    // Hasta contar con un ámbito de cuenta validada, ningún driver ni repositorio offline se abre.
    single<ProductoRepository> { ProductoRepositorioNoDisponible() }
}

val domainModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RestaurarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { ListarCategoriasUseCase(get()) }
}

val presentationModule = module {
    viewModelOf(::SesionViewModel)
    viewModelOf(::ProductoViewModel)
}

expect val platformModule: Module

fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        modules(listOf(dataModule, domainModule, presentationModule, platformModule) + extraModules)
    }
}
