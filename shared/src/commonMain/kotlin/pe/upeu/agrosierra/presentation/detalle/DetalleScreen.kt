package pe.upeu.agrosierra.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.presentation.comun.EstadoCargando
import pe.upeu.agrosierra.presentation.comun.EstadoVacio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    viewModel: DetalleViewModel,
    aviso: String?,
    alConsumirAviso: () -> Unit,
    alVolver: () -> Unit,
    alRegistrar: () -> Unit,
    alEditar: (Labor) -> Unit,
) {
    val estado by viewModel.uiState.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val avisos = remember { SnackbarHostState() }
    var confirmarCierre by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Labor?>(null) }

    // Se consume después de mostrarlo: consumirlo antes cambia la clave y cancela el snackbar.
    LaunchedEffect(mensaje) {
        mensaje?.let {
            avisos.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }
    LaunchedEffect(aviso) {
        aviso?.let {
            avisos.showSnackbar(it)
            alConsumirAviso()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.lote?.let { "${it.codigo} · ${it.nombre}" } ?: "Lote") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { relleno ->
        val lote = estado.lote
        when {
            estado.cargando -> EstadoCargando(Modifier.padding(relleno))
            lote == null -> EstadoVacio("El lote no existe o fue retirado del padron", Modifier.padding(relleno))
            else -> Column(Modifier.fillMaxSize().padding(relleno)) {
                if (estado.procesando) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { DatosLote(lote) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (estado.puedeRegistrar) {
                                Button(onClick = alRegistrar) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("Registrar labor")
                                }
                            }
                            if (estado.puedeCerrarCampania) {
                                OutlinedButton(onClick = { confirmarCierre = true }, enabled = !estado.procesando) {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                    Text("Cerrar campania")
                                }
                            }
                        }
                    }
                    item { Text("Labores (${estado.labores.size})", style = MaterialTheme.typography.titleMedium) }
                    if (estado.labores.isEmpty()) {
                        item { Text("Este lote aun no tiene labores registradas.") }
                    }
                    items(estado.labores, key = { it.id }) { labor ->
                        TarjetaLabor(
                            labor = labor,
                            puedeEditar = estado.puedeEditar,
                            // Una labor pendiente que nunca se envió siempre se puede descartar.
                            puedeEliminar = estado.puedeEliminar || labor.id < 0,
                            alEditar = { alEditar(labor) },
                            alEliminar = { porEliminar = labor },
                        )
                    }
                }
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("Cerrar campania") },
            text = { Text("Despues de cerrarla no se podran registrar ni corregir labores en este lote.") },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; viewModel.cerrarCampania() }) { Text("Cerrar campania") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } },
        )
    }
    porEliminar?.let { labor ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text(if (labor.id < 0) "Descartar labor pendiente" else "Eliminar labor") },
            text = { Text("${labor.tipo.name} del ${labor.fecha} (${labor.responsable})") },
            confirmButton = {
                TextButton(onClick = { porEliminar = null; viewModel.eliminar(labor) }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DatosLote(lote: Lote) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cultivo: ${lote.cultivo}")
            Text("Valle: ${lote.valle}")
            Text("Hectareas: ${lote.hectareas}")
            Text("Dosis maxima: ${lote.dosisMaximaPorHectarea} por ha")
            Text(
                text = if (lote.campaniaCerrada) "Campania cerrada" else "Campania abierta",
                color = if (lote.campaniaCerrada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TarjetaLabor(
    labor: Labor,
    puedeEditar: Boolean,
    puedeEliminar: Boolean,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${labor.tipo.name} · ${labor.fecha}", style = MaterialTheme.typography.titleSmall)
                Text("Responsable: ${labor.responsable}")
                Text("Cantidad: ${labor.cantidad}")
                if (labor.observacion.isNotBlank()) Text(labor.observacion, style = MaterialTheme.typography.bodySmall)
                if (labor.pendiente) AssistChip(onClick = {}, label = { Text("Pendiente de envio") })
            }
            if (puedeEditar) {
                IconButton(onClick = alEditar) { Icon(Icons.Default.Edit, contentDescription = "Corregir") }
            }
            if (puedeEliminar) {
                IconButton(onClick = alEliminar) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
        }
    }
}
