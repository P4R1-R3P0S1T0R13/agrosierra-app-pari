package pe.upeu.agrosierra.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Privilegios
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.CerrarCampaniaUseCase
import pe.upeu.agrosierra.domain.usecase.EliminarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.ObservarDetalleLoteUseCase
import pe.upeu.agrosierra.presentation.comun.paraUsuario

data class DetalleUiState(
    val cargando: Boolean = true,
    val lote: Lote? = null,
    val labores: List<Labor> = emptyList(),
    /** Qué acciones se OFRECEN según los privilegios; el servicio igual las verifica (403). */
    val puedeRegistrar: Boolean = false,
    val puedeEditar: Boolean = false,
    val puedeEliminar: Boolean = false,
    val puedeCerrarCampania: Boolean = false,
    val procesando: Boolean = false,
)

class DetalleViewModel(
    private val loteId: Long,
    observarDetalle: ObservarDetalleLoteUseCase,
    sesiones: SesionRepository,
    private val cerrarCampania: CerrarCampaniaUseCase,
    private val eliminarLabor: EliminarLaborUseCase,
) : ViewModel() {

    private val procesando = MutableStateFlow(false)
    private val mensajeInterno = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = mensajeInterno

    val uiState: StateFlow<DetalleUiState> = combine(
        observarDetalle.lote(loteId),
        observarDetalle.labores(loteId),
        sesiones.sesion,
        procesando,
    ) { lote, labores, sesion, ocupado ->
        val abierta = lote != null && !lote.campaniaCerrada
        DetalleUiState(
            cargando = false,
            lote = lote,
            labores = labores,
            puedeRegistrar = abierta && sesion.permite(Privilegios.LABOR_CREAR),
            puedeEditar = abierta && sesion.permite(Privilegios.LABOR_EDITAR),
            puedeEliminar = sesion.permite(Privilegios.LABOR_ELIMINAR),
            puedeCerrarCampania = abierta && sesion.permite(Privilegios.CAMPANIA_CERRAR),
            procesando = ocupado,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleUiState())

    fun cerrarCampania() = ejecutar {
        when (val r = cerrarCampania(loteId)) {
            is Resultado.Exito -> "Campania del lote ${r.datos.codigo} cerrada"
            is Resultado.Fallo -> r.error.paraUsuario()
        }
    }

    fun eliminar(labor: Labor) = ejecutar {
        when (val r = eliminarLabor(labor.id)) {
            is Resultado.Exito -> if (labor.id < 0) "Labor pendiente descartada" else "Labor eliminada"
            is Resultado.Fallo -> r.error.paraUsuario()
        }
    }

    fun mensajeMostrado() = mensajeInterno.update { null }

    private fun ejecutar(accion: suspend () -> String) {
        if (procesando.value) return
        procesando.value = true
        viewModelScope.launch {
            mensajeInterno.value = accion()
            procesando.value = false
        }
    }

    private fun Sesion?.permite(privilegio: String) = this?.tiene(privilegio) == true
}
