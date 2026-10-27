package pe.upeu.agrosierra.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.usecase.IniciarSesionUseCase
import pe.upeu.agrosierra.presentation.comun.paraUsuario

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val errorUsuario: String? = null,
    val errorContrasena: String? = null,
    val mensaje: String? = null,
)

class LoginViewModel(private val iniciarSesion: IniciarSesionUseCase) : ViewModel() {

    private val estado = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = estado.asStateFlow()

    fun alCambiarUsuario(valor: String) = estado.update { it.copy(usuario = valor, errorUsuario = null, mensaje = null) }

    fun alCambiarContrasena(valor: String) =
        estado.update { it.copy(contrasena = valor, errorContrasena = null, mensaje = null) }

    /** Al tener éxito, la sesión cambia y la navegación pasa sola al área protegida. */
    fun ingresar() {
        val actual = estado.value
        if (actual.cargando) return
        estado.update { it.copy(cargando = true, mensaje = null) }
        viewModelScope.launch {
            val r = iniciarSesion(actual.usuario, actual.contrasena)
            estado.update {
                when (r) {
                    is Resultado.Exito -> it.copy(cargando = false, contrasena = "")
                    is Resultado.Fallo -> when (val e = r.error) {
                        is ErrorApi.Validacion -> it.copy(
                            cargando = false,
                            errorUsuario = e.campos["usuario"],
                            errorContrasena = e.campos["contrasena"],
                            mensaje = if (e.campos.isEmpty()) e.mensaje else null,
                        )
                        // Mensajes distintos para credenciales incorrectas y para falta de conexión (RF-A01).
                        is ErrorApi.NoAutorizado -> it.copy(cargando = false, mensaje = "Usuario o contrasena incorrectos")
                        ErrorApi.SinConexion -> it.copy(
                            cargando = false,
                            mensaje = "Sin conexion con el servidor. Verifique su red e intente de nuevo.",
                        )
                        else -> it.copy(cargando = false, mensaje = e.paraUsuario())
                    }
                }
            }
        }
    }
}
