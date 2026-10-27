package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import pe.upeu.agrosierra.data.local.GestorSesion
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.aSesion
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository

class SesionRepositoryImpl(
    private val api: AgroApi,
    private val gestor: GestorSesion,
    private val ahora: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : SesionRepository {

    override val sesion: StateFlow<Sesion?> = gestor.sesion

    override suspend fun iniciarSesion(usuario: String, contrasena: String): Resultado<Sesion> =
        when (val r = api.login(usuario, contrasena)) {
            is Resultado.Exito -> {
                val nueva = r.datos.aSesion()
                gestor.guardar(nueva)
                Resultado.Exito(nueva)
            }
            is Resultado.Fallo -> r
        }

    override suspend fun restaurar(): Sesion? {
        val guardada = gestor.leerGuardada() ?: return null
        if (guardada.expiraEn <= ahora()) {
            gestor.invalidar()
            return null
        }
        gestor.activar(guardada)
        return guardada
    }

    override suspend fun refrescarUsuario(): Resultado<Sesion> {
        val actual = sesion.value ?: return Resultado.Fallo(ErrorApi.NoAutorizado())
        return when (val r = api.usuarioActual()) {
            is Resultado.Exito -> {
                val refrescada = actual.copy(
                    nombre = r.datos.nombre,
                    rol = r.datos.rol,
                    privilegios = r.datos.privilegios,
                )
                gestor.guardar(refrescada)
                Resultado.Exito(refrescada)
            }
            is Resultado.Fallo -> r
        }
    }

    override suspend fun cerrarSesion() = gestor.invalidar()
}
