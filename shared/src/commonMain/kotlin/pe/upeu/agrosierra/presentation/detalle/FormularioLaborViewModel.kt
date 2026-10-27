package pe.upeu.agrosierra.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.GuardarLaborUseCase
import pe.upeu.agrosierra.domain.usecase.ObtenerLaborUseCase
import pe.upeu.agrosierra.presentation.comun.paraUsuario

data class FormularioUiState(
    val cargando: Boolean = true,
    val esCorreccion: Boolean = false,
    val tipo: TipoLabor = TipoLabor.RIEGO,
    val fecha: String = "",
    val responsable: String = "",
    val cantidad: String = "",
    val observacion: String = "",
    /** Mensaje bajo cada campo: de la validación local o de validationErrors del servicio. */
    val errores: Map<String, String> = emptyMap(),
    val mensaje: String? = null,
    val guardando: Boolean = false,
    /** Al guardar: texto que la pantalla anterior mostrará; la pantalla se cierra. */
    val terminado: String? = null,
)

class FormularioLaborViewModel(
    private val loteId: Long,
    private val laborId: Long,
    private val guardarLabor: GuardarLaborUseCase,
    private val obtenerLabor: ObtenerLaborUseCase,
    private val sesiones: SesionRepository,
) : ViewModel() {

    private val estado = MutableStateFlow(FormularioUiState())
    val uiState: StateFlow<FormularioUiState> = estado.asStateFlow()

    init {
        viewModelScope.launch {
            val existente = if (laborId != GuardarLaborUseCase.NUEVA) obtenerLabor(laborId) else null
            estado.value = if (existente != null) {
                FormularioUiState(
                    cargando = false,
                    esCorreccion = true,
                    tipo = existente.tipo,
                    fecha = existente.fecha,
                    responsable = existente.responsable,
                    cantidad = existente.cantidad.toString(),
                    observacion = existente.observacion,
                )
            } else {
                FormularioUiState(
                    cargando = false,
                    fecha = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString(),
                    responsable = sesiones.sesion.value?.nombre.orEmpty(),
                    mensaje = if (laborId != GuardarLaborUseCase.NUEVA) "La labor ya no existe" else null,
                )
            }
        }
    }

    fun alCambiarTipo(tipo: TipoLabor) = estado.update { it.copy(tipo = tipo, errores = it.errores - "tipo") }
    fun alCambiarFecha(v: String) = estado.update { it.copy(fecha = v, errores = it.errores - "fecha") }
    fun alCambiarResponsable(v: String) = estado.update { it.copy(responsable = v, errores = it.errores - "responsable") }
    fun alCambiarCantidad(v: String) = estado.update { it.copy(cantidad = v, errores = it.errores - "cantidad") }
    fun alCambiarObservacion(v: String) = estado.update { it.copy(observacion = v, errores = it.errores - "observacion") }

    fun guardar() {
        val actual = estado.value
        if (actual.guardando) return
        val cantidad = actual.cantidad.replace(',', '.').toDoubleOrNull()
        if (cantidad == null) {
            estado.update { it.copy(errores = it.errores + ("cantidad" to "Ingrese un numero valido")) }
            return
        }
        val labor = Labor(
            id = laborId,
            loteId = loteId,
            tipo = actual.tipo,
            fecha = actual.fecha.trim(),
            responsable = actual.responsable,
            cantidad = cantidad,
            observacion = actual.observacion,
            pendiente = false,
        )
        estado.update { it.copy(guardando = true, mensaje = null, errores = emptyMap()) }
        viewModelScope.launch {
            val r = guardarLabor(labor)
            estado.update {
                when (r) {
                    is Resultado.Exito -> it.copy(
                        guardando = false,
                        terminado = if (r.datos.pendiente) {
                            "Guardada sin conexion: quedo pendiente de envio"
                        } else {
                            "Labor guardada en el servidor"
                        },
                    )
                    is Resultado.Fallo -> when (val e = r.error) {
                        // 400: cada mensaje va bajo el campo que lo originó.
                        is ErrorApi.Validacion -> it.copy(
                            guardando = false,
                            errores = e.campos,
                            mensaje = if (e.campos.keys.none { k -> k in CAMPOS }) e.mensaje else null,
                        )
                        // 404 al corregir: la labor ya no existe; se vuelve al detalle, que ya se refrescó.
                        is ErrorApi.NoEncontrado -> it.copy(guardando = false, terminado = e.paraUsuario())
                        // 403 y 409: se muestra el motivo devuelto por el servicio.
                        else -> it.copy(guardando = false, mensaje = e.paraUsuario())
                    }
                }
            }
        }
    }

    private companion object {
        val CAMPOS = setOf("tipo", "fecha", "responsable", "cantidad", "observacion")
    }
}
