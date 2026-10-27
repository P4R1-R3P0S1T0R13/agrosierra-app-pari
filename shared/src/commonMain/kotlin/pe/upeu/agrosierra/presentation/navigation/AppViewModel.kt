package pe.upeu.agrosierra.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.RestaurarSesionUseCase
import pe.upeu.agrosierra.presentation.sincronizacion.GestorSincronizacion

enum class EstadoApp { INICIANDO, SIN_SESION, CON_SESION }

/** Decide la pantalla raíz: al reabrir se restaura la sesión si el token sigue vigente (RF-A02). */
class AppViewModel(
    restaurarSesion: RestaurarSesionUseCase,
    sesiones: SesionRepository,
    gestor: GestorSincronizacion,
    private val navegador: Navegador,
) : ViewModel() {

    private val listo = MutableStateFlow(false)

    val estado: StateFlow<EstadoApp> = combine(listo, sesiones.sesion) { iniciado, sesion ->
        when {
            !iniciado -> EstadoApp.INICIANDO
            sesion == null -> EstadoApp.SIN_SESION
            else -> EstadoApp.CON_SESION
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoApp.INICIANDO)

    init {
        viewModelScope.launch {
            restaurarSesion()
            listo.value = true
        }
        viewModelScope.launch {
            // Al cerrar sesión o recibir 401 la pila vuelve a empezar desde el padrón.
            sesiones.sesion.collect { if (it == null) navegador.reiniciar() }
        }
        gestor.iniciarAutomatica()
    }
}
