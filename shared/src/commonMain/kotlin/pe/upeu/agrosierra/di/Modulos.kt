package pe.upeu.agrosierra.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.local.GestorSesion
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.crearClienteHttp
import pe.upeu.agrosierra.data.repository.LaborRepositoryImpl
import pe.upeu.agrosierra.data.repository.LoteRepositoryImpl
import pe.upeu.agrosierra.data.repository.SesionRepositoryImpl
import pe.upeu.agrosierra.data.repository.SincronizacionRepositoryImpl
import pe.upeu.agrosierra.domain.repository.LaborRepository
import pe.upeu.agrosierra.domain.repository.LoteRepository
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.repository.SincronizacionRepository
import pe.upeu.agrosierra.domain.usecase.CerrarCampaniaUseCase
import pe.upeu.agrosierra.domain.usecase.CerrarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.EliminarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.GuardarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.IniciarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarDetalleLoteUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarLotesUseCase
import pe.upeu.agrosierra.domain.usecase.ObtenerLaborUseCase
import pe.upeu.agrosierra.domain.usecase.RestaurarSesionUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import pe.upeu.agrosierra.presentation.detalle.DetalleViewModel
import pe.upeu.agrosierra.presentation.detalle.FormularioLaborViewModel
import pe.upeu.agrosierra.presentation.listado.ListadoViewModel
import pe.upeu.agrosierra.presentation.navigation.AppViewModel
import pe.upeu.agrosierra.presentation.navigation.Navegador
import pe.upeu.agrosierra.presentation.perfil.PerfilViewModel
import pe.upeu.agrosierra.presentation.sesion.LoginViewModel
import pe.upeu.agrosierra.presentation.sincronizacion.GestorSincronizacion

/**
 * Red: una única instancia de HttpClient para toda la app. El motor (HttpClientEngine),
 * ApiConfig y AlmacenSeguro los aporta el platformModule de cada plataforma.
 */
val redModule = module {
    single { GestorSesion(get()) }
    single {
        val gestor = get<GestorSesion>()
        crearClienteHttp(
            motor = get(),
            config = get(),
            leerToken = gestor::token,
            alRecibirNoAutorizado = gestor::invalidar,
        )
    }
    single { AgroApi(get()) }
}

val dataModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { BaseLocal(get()) }
    single<SesionRepository> { SesionRepositoryImpl(get(), get()) }
    single<LoteRepository> { LoteRepositoryImpl(get(), get()) }
    single<LaborRepository> { LaborRepositoryImpl(get(), get()) }
    single<SincronizacionRepository> { SincronizacionRepositoryImpl(get(), get()) }
}

val domainModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RestaurarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
    factory { ObservarLotesUseCase(get()) }
    factory { ObservarDetalleLoteUseCase(get(), get()) }
    factory { CerrarCampaniaUseCase(get()) }
    factory { GuardarLaborUseCase(get()) }
    factory { ObtenerLaborUseCase(get()) }
    factory { EliminarLaborUseCase(get()) }
    factory { SincronizarUseCase(get()) }
}

val presentationModule = module {
    single { Navegador() }
    single { GestorSincronizacion(get(), get(), get(), get()) }
    viewModel { AppViewModel(get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { ListadoViewModel(get(), get(), get()) }
    viewModel { (loteId: Long) -> DetalleViewModel(loteId, get(), get(), get(), get()) }
    viewModel { (loteId: Long, laborId: Long) -> FormularioLaborViewModel(loteId, laborId, get(), get(), get()) }
    viewModel { PerfilViewModel(get(), get()) }
}

/** Módulos del código común; la app los combina con su platformModule. */
val modulosComunes = listOf(redModule, dataModule, domainModule, presentationModule)
