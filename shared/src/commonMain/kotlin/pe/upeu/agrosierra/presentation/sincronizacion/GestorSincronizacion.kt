package pe.upeu.agrosierra.presentation.sincronizacion

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.ResumenSincronizacion
import pe.upeu.agrosierra.domain.repository.MonitorConexion
import pe.upeu.agrosierra.domain.repository.SesionRepository
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase

data class EstadoSincronizacion(
    val enCurso: Boolean = false,
    val etapa: SincronizarUseCase.Etapa? = null,
    val resumen: ResumenSincronizacion? = null,
    val error: ErrorApi? = null,
    val conectado: Boolean = true,
)

/**
 * Coordina la sincronización de toda la app: manual (botón) y automática cuando
 * vuelve la conexión o se inicia sesión. Un candado evita dos sincronizaciones a la vez.
 */
class GestorSincronizacion(
    private val sincronizar: SincronizarUseCase,
    private val monitor: MonitorConexion,
    private val sesion: SesionRepository,
    private val alcance: CoroutineScope,
) {
    private val estadoInterno = MutableStateFlow(EstadoSincronizacion())
    val estado: StateFlow<EstadoSincronizacion> = estadoInterno.asStateFlow()

    private val candado = Mutex()
    private var automaticaIniciada = false

    fun iniciarAutomatica() {
        if (automaticaIniciada) return
        automaticaIniciada = true
        alcance.launch {
            combine(monitor.conectado, sesion.sesion) { conectado, actual -> conectado to (actual != null) }
                .distinctUntilChanged()
                .collect { (conectado, conSesion) ->
                    estadoInterno.update { it.copy(conectado = conectado) }
                    if (conectado && conSesion) ejecutar()
                }
        }
    }

    fun sincronizarAhora() {
        alcance.launch { ejecutar() }
    }

    fun limpiarAviso() = estadoInterno.update { it.copy(resumen = null, error = null) }

    private suspend fun ejecutar() {
        if (!candado.tryLock()) return
        try {
            estadoInterno.update { it.copy(enCurso = true, error = null, resumen = null) }
            val r = sincronizar { etapa -> estadoInterno.update { it.copy(etapa = etapa) } }
            estadoInterno.update {
                when (r) {
                    is Resultado.Exito -> it.copy(enCurso = false, etapa = null, resumen = r.datos)
                    is Resultado.Fallo -> it.copy(enCurso = false, etapa = null, error = r.error)
                }
            }
        } finally {
            candado.unlock()
        }
    }
}
