package pe.upeu.agrosierra.presentation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface Ruta {
    data object Padron : Ruta
    data class Detalle(val loteId: Long) : Ruta
    /** [laborId] = 0 para registrar; distinto de 0 para corregir. */
    data class Formulario(val loteId: Long, val laborId: Long = 0) : Ruta
    data object Perfil : Ruta
}

/** Entrada de la pila: [id] único para que cada pantalla abierta tenga su propio ViewModel. */
data class Entrada(val id: Long, val ruta: Ruta)

/** Pila de navegación del área protegida, compartida por Compose y por el botón atrás de Android. */
class Navegador {
    private var siguienteId = 0L
    private val pilaInterna = MutableStateFlow(listOf(nueva(Ruta.Padron)))
    val pila: StateFlow<List<Entrada>> = pilaInterna.asStateFlow()

    private val avisoInterno = MutableStateFlow<String?>(null)
    /** Mensaje que una pantalla deja para la anterior (por ejemplo, "labor guardada"). */
    val aviso: StateFlow<String?> = avisoInterno.asStateFlow()

    fun ir(ruta: Ruta) = pilaInterna.update { it + nueva(ruta) }

    /** Devuelve false si ya está en la raíz (la actividad decide cerrar la app). */
    fun atras(aviso: String? = null): Boolean {
        if (pilaInterna.value.size <= 1) return false
        pilaInterna.update { it.dropLast(1) }
        if (aviso != null) avisoInterno.value = aviso
        return true
    }

    fun consumirAviso() {
        avisoInterno.value = null
    }

    fun reiniciar() {
        pilaInterna.value = listOf(nueva(Ruta.Padron))
        avisoInterno.value = null
    }

    private fun nueva(ruta: Ruta) = Entrada(siguienteId++, ruta)
}
