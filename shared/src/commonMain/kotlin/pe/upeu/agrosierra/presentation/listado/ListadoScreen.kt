package pe.upeu.agrosierra.presentation.listado

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import pe.upeu.agrosierra.presentation.comun.EstadoCargando
import pe.upeu.agrosierra.presentation.comun.EstadoVacio
import pe.upeu.agrosierra.presentation.comun.admiteReintento
import pe.upeu.agrosierra.presentation.comun.paraUsuario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListadoScreen(
    alAbrirLote: (Long) -> Unit,
    alAbrirPerfil: () -> Unit,
    viewModel: ListadoViewModel = koinViewModel(),
) {
    val estado by viewModel.uiState.collectAsState()
    val avisos = remember { SnackbarHostState() }
    val sync = estado.sincronizacion

    // Resumen de lo enviado y lo recibido, o el motivo del fallo.
    LaunchedEffect(sync.resumen, sync.error) {
        val resumen = sync.resumen
        val error = sync.error
        when {
            resumen != null -> {
                val texto = buildString {
                    append("Sincronizado: ${resumen.enviadas} enviadas, ")
                    append("${resumen.lotesRecibidos} lotes y ${resumen.laboresRecibidas} labores recibidas")
                    if (resumen.rechazadas.isNotEmpty()) append(". Rechazadas: ${resumen.rechazadas.joinToString(" | ")}")
                }
                viewModel.avisoMostrado()
                avisos.showSnackbar(texto)
            }
            error != null -> {
                viewModel.avisoMostrado()
                val accion = if (error.admiteReintento()) "Reintentar" else null
                val r = avisos.showSnackbar(error.paraUsuario(), actionLabel = accion)
                if (r == SnackbarResult.ActionPerformed) viewModel.sincronizar()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Padron de lotes") },
                actions = {
                    IconButton(onClick = viewModel::sincronizar, enabled = !sync.enCurso) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                    IconButton(onClick = alAbrirPerfil) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            if (sync.enCurso) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    text = when (sync.etapa) {
                        SincronizarUseCase.Etapa.ENVIANDO -> "Enviando labores pendientes..."
                        else -> "Descargando lotes y labores..."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            BarraEstado(estado)

            OutlinedTextField(
                value = estado.texto,
                onValueChange = viewModel::alBuscar,
                placeholder = { Text("Buscar por nombre o codigo") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = estado.valle == null,
                    onClick = { viewModel.alElegirValle(null) },
                    label = { Text("Todos") },
                )
                estado.valles.forEach { valle ->
                    FilterChip(
                        selected = estado.valle == valle,
                        onClick = { viewModel.alElegirValle(valle) },
                        label = { Text(valle) },
                    )
                }
            }

            when {
                estado.cargando -> EstadoCargando()
                estado.lotes.isEmpty() && estado.hayFiltro -> EstadoVacio("Ningun lote coincide con el filtro")
                estado.lotes.isEmpty() -> EstadoVacio("Aun no hay lotes guardados. Sincronice cuando tenga conexion.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(estado.lotes, key = { it.id }) { lote -> TarjetaLote(lote) { alAbrirLote(lote.id) } }
                }
            }
        }
    }
}

@Composable
private fun BarraEstado(estado: ListadoUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Ultima sincronizacion: ${estado.ultimaSincronizacion?.replace('T', ' ') ?: "nunca"}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (!estado.sincronizacion.conectado) AssistChip(onClick = {}, label = { Text("Sin conexion") })
        if (estado.pendientes > 0) AssistChip(onClick = {}, label = { Text("${estado.pendientes} pendientes") })
    }
}

@Composable
private fun TarjetaLote(lote: Lote, alPulsar: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = alPulsar)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${lote.codigo} · ${lote.nombre}", style = MaterialTheme.typography.titleMedium)
            Text("${lote.cultivo} · Valle ${lote.valle} · ${lote.hectareas} ha", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (lote.campaniaCerrada) "Campania cerrada" else "Campania abierta",
                color = if (lote.campaniaCerrada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
