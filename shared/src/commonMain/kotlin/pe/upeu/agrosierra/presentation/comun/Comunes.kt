package pe.upeu.agrosierra.presentation.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.upeu.agrosierra.domain.model.ErrorApi

/** Texto que la interfaz muestra para cada error tipado (sección 3.5). */
fun ErrorApi.paraUsuario(): String = when (this) {
    is ErrorApi.Validacion -> mensaje
    is ErrorApi.NoAutorizado -> "Su sesion vencio. Ingrese nuevamente."
    is ErrorApi.SinPrivilegio -> "Operacion no autorizada: $mensaje"
    is ErrorApi.NoEncontrado -> "$mensaje. Se actualizo el listado."
    is ErrorApi.Conflicto -> mensaje
    is ErrorApi.Servidor -> "El servidor tuvo un problema. Intente nuevamente."
    ErrorApi.SinConexion -> "Sin conexion. Se enviara al recuperar la senal."
    ErrorApi.TiempoAgotado -> "El servidor tardo demasiado en responder."
    is ErrorApi.Desconocido -> mensaje
}

/** Solo el 500 ofrece reintentar (sección 3.5). */
fun ErrorApi.admiteReintento(): Boolean = this is ErrorApi.Servidor || this is ErrorApi.TiempoAgotado

@Composable
fun EstadoCargando(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { CircularProgressIndicator() }
}

@Composable
fun EstadoVacio(mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun EstadoError(mensaje: String, alReintentar: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (alReintentar != null) Button(onClick = alReintentar) { Text("Reintentar") }
    }
}
