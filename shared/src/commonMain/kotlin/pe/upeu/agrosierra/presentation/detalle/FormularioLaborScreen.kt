package pe.upeu.agrosierra.presentation.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.presentation.comun.EstadoCargando

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioLaborScreen(
    viewModel: FormularioLaborViewModel,
    alTerminar: (String) -> Unit,
    alVolver: () -> Unit,
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.terminado) { estado.terminado?.let(alTerminar) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esCorreccion) "Corregir labor" else "Registrar labor") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { relleno ->
        if (estado.cargando) {
            EstadoCargando(Modifier.padding(relleno))
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tipo de labor", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TipoLabor.entries.forEach { tipo ->
                    FilterChip(
                        selected = estado.tipo == tipo,
                        onClick = { viewModel.alCambiarTipo(tipo) },
                        label = { Text(tipo.name) },
                    )
                }
            }
            estado.errores["tipo"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Campo("Fecha (AAAA-MM-DD)", estado.fecha, estado.errores["fecha"], viewModel::alCambiarFecha)
            Campo("Responsable", estado.responsable, estado.errores["responsable"], viewModel::alCambiarResponsable)
            Campo(
                etiqueta = if (estado.tipo == TipoLabor.FUMIGACION) "Cantidad aplicada (dosis)" else "Cantidad",
                valor = estado.cantidad,
                error = estado.errores["cantidad"],
                alCambiar = viewModel::alCambiarCantidad,
                teclado = KeyboardType.Decimal,
            )
            Campo("Observacion", estado.observacion, estado.errores["observacion"], viewModel::alCambiarObservacion, lineas = 3)

            estado.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = viewModel::guardar,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
            Text(
                "Sin conexion la labor se guarda en el telefono y se envia al sincronizar.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    error: String?,
    alCambiar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth(),
    )
}
