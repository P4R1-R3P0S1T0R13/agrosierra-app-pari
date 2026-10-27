package pe.upeu.agrosierra.presentation.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(alVolver: () -> Unit, viewModel: PerfilViewModel = koinViewModel()) {
    val sesion by viewModel.sesion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { relleno ->
        val actual = sesion ?: return@Scaffold
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(actual.nombre, style = MaterialTheme.typography.titleLarge)
                    Text("Usuario: ${actual.usuario}")
                    Text("Rol: ${actual.rol}")
                }
            }
            Text("Privilegios", style = MaterialTheme.typography.titleMedium)
            actual.privilegios.sorted().forEach { privilegio ->
                ListItem(
                    headlineContent = { Text(privilegio) },
                    leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                )
            }
            Button(
                onClick = viewModel::cerrarSesion,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Cerrar sesion") }
        }
    }
}
