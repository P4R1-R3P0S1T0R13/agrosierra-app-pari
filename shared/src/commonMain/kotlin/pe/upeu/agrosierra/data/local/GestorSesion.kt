package pe.upeu.agrosierra.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.agrosierra.domain.model.Sesion

/**
 * Dueño del estado de sesión. Lo usan el cliente HTTP (para leer el token y reaccionar al 401)
 * y el repositorio de sesión; así el cliente no depende del API y no hay ciclos en Koin.
 */
class GestorSesion(private val almacen: AlmacenSeguro) {

    private val estado = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = estado.asStateFlow()

    suspend fun token(): String? = estado.value?.token ?: almacen.leer(TOKEN)

    suspend fun guardar(sesion: Sesion) {
        almacen.guardar(
            mapOf(
                TOKEN to sesion.token,
                USUARIO to sesion.usuario,
                NOMBRE to sesion.nombre,
                ROL to sesion.rol,
                PRIVILEGIOS to sesion.privilegios.joinToString(","),
                EXPIRA_EN to sesion.expiraEn.toString(),
            ),
        )
        estado.value = sesion
    }

    /** Lee la sesión guardada sin validarla; la vigencia la decide el repositorio. */
    suspend fun leerGuardada(): Sesion? {
        val token = almacen.leer(TOKEN) ?: return null
        return Sesion(
            token = token,
            usuario = almacen.leer(USUARIO).orEmpty(),
            rol = almacen.leer(ROL).orEmpty(),
            privilegios = almacen.leer(PRIVILEGIOS).orEmpty().split(",").filter { it.isNotBlank() }.toSet(),
            nombre = almacen.leer(NOMBRE).orEmpty(),
            expiraEn = almacen.leer(EXPIRA_EN)?.toLongOrNull() ?: 0,
        )
    }

    fun activar(sesion: Sesion) {
        estado.value = sesion
    }

    /** Cierre de sesión o 401: borra el token del almacenamiento seguro y limpia el estado. */
    suspend fun invalidar() {
        almacen.borrarTodo()
        estado.value = null
    }

    private companion object {
        const val TOKEN = "token"
        const val USUARIO = "usuario"
        const val NOMBRE = "nombre"
        const val ROL = "rol"
        const val PRIVILEGIOS = "privilegios"
        const val EXPIRA_EN = "expira_en"
    }
}
