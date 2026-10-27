package pe.upeu.agrosierra.presentation.listado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.usecase.ObservarLotesUseCase
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import pe.upeu.agrosierra.presentation.sincronizacion.EstadoSincronizacion
import pe.upeu.agrosierra.presentation.sincronizacion.GestorSincronizacion

data class ListadoUiState(
    val cargando: Boolean = true,
    val lotes: List<Lote> = emptyList(),
    val valles: List<String> = emptyList(),
    val valle: String? = null,
    val texto: String = "",
    val ultimaSincronizacion: String? = null,
    val pendientes: Long = 0,
    val sincronizacion: EstadoSincronizacion = EstadoSincronizacion(),
) {
    val hayFiltro: Boolean get() = valle != null || texto.isNotBlank()
}

private data class Filtro(val valle: String?, val texto: String)

/** El padrón se alimenta SOLO de la base local; la red solo llega a través de la sincronización. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListadoViewModel(
    private val observarLotes: ObservarLotesUseCase,
    sincronizar: SincronizarUseCase,
    private val gestor: GestorSincronizacion,
) : ViewModel() {

    private val filtro = MutableStateFlow(Filtro(valle = null, texto = ""))

    private val lotes = filtro.flatMapLatest { observarLotes(it.valle, it.texto) }

    val uiState: StateFlow<ListadoUiState> = combine(
        combine(filtro, lotes, ::Pair),
        observarLotes.valles(),
        observarLotes.ultimaSincronizacion(),
        sincronizar.pendientes(),
        gestor.estado,
    ) { (f, lista), valles, ultima, pendientes, sync ->
        ListadoUiState(
            cargando = false,
            lotes = lista,
            valles = valles,
            valle = f.valle,
            texto = f.texto,
            ultimaSincronizacion = ultima,
            pendientes = pendientes,
            sincronizacion = sync,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListadoUiState())

    fun alBuscar(texto: String) = filtro.update { it.copy(texto = texto) }

    fun alElegirValle(valle: String?) = filtro.update { it.copy(valle = valle) }

    fun sincronizar() = gestor.sincronizarAhora()

    fun avisoMostrado() = gestor.limpiarAviso()
}
