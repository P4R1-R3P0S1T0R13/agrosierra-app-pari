package pe.upeu.agrosierra.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeCampo = Color(0xFF2E7D32)
private val VerdeClaro = Color(0xFF81C784)
private val Tierra = Color(0xFF8D6E63)
private val Ambar = Color(0xFFF9A825)

private val Claro = lightColorScheme(
    primary = VerdeCampo,
    onPrimary = Color.White,
    secondary = Tierra,
    tertiary = Ambar,
)

private val Oscuro = darkColorScheme(
    primary = VerdeClaro,
    secondary = Color(0xFFBCAAA4),
    tertiary = Ambar,
)

@Composable
fun AgroTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}
