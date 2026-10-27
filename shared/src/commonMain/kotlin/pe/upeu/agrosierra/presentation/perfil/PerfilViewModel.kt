package pe.upeu.agrosierra.presentation.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.CerrarSesionUseCase

class PerfilViewModel(
    private val sesiones: SesionRepository,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
) : ViewModel() {

    val sesion: StateFlow<Sesion?> = sesiones.sesion

    init {
        // RF-B07: con conexión se refrescan los privilegios desde /usuarios/me; sin ella se usan los guardados.
        viewModelScope.launch { sesiones.refrescarUsuario() }
    }

    /** Borra el token del almacenamiento seguro; la navegación vuelve sola al inicio de sesión. */
    fun cerrarSesion() {
        viewModelScope.launch { cerrarSesionUseCase() }
    }
}
