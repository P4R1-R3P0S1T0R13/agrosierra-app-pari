package pe.upeu.agrosierra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.upeu.agrosierra.presentation.navigation.App
import pe.upeu.agrosierra.presentation.navigation.Navegador

class MainActivity : ComponentActivity() {

    private val navegador: Navegador by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // El botón atrás del sistema recorre la misma pila que usa Compose.
        onBackPressedDispatcher.addCallback(this) {
            if (!navegador.atras()) finish()
        }
        setContent { App() }
    }
}
