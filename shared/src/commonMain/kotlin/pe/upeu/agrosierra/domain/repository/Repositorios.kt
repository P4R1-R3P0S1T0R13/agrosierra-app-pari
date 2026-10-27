package pe.upeu.agrosierra.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.ResumenDescarga
import pe.upeu.agrosierra.domain.model.ResumenEnvio
import pe.upeu.agrosierra.domain.model.Sesion

interface SesionRepository {
    /** Sesión vigente; pasa a null al cerrar sesión o cuando el servicio responde 401. */
    val sesion: StateFlow<Sesion?>
    suspend fun iniciarSesion(usuario: String, contrasena: String): Resultado<Sesion>
    /** Recupera la sesión guardada si el token sigue vigente. */
    suspend fun restaurar(): Sesion?
    suspend fun refrescarUsuario(): Resultado<Sesion>
    suspend fun cerrarSesion()
}

interface LoteRepository {
    /** Padrón leído de la base local: [valle] null significa todos. */
    fun observarLotes(valle: String?, texto: String): Flow<List<Lote>>
    fun observarValles(): Flow<List<String>>
    fun observarLote(id: Long): Flow<Lote?>
    fun observarUltimaSincronizacion(): Flow<String?>
    suspend fun cerrarCampania(id: Long): Resultado<Lote>
}

interface LaborRepository {
    fun observarLabores(loteId: Long): Flow<List<Labor>>
    suspend fun obtener(id: Long): Labor?
    /** Guarda primero en local (pendiente) y luego intenta enviarla; sin red queda pendiente. */
    suspend fun registrar(labor: Labor): Resultado<Labor>
    suspend fun corregir(labor: Labor): Resultado<Labor>
    suspend fun eliminar(id: Long): Resultado<Unit>
}

interface SincronizacionRepository {
    fun observarPendientes(): Flow<Long>
    suspend fun enviarPendientes(): ResumenEnvio
    suspend fun descargarCambios(): Resultado<ResumenDescarga>
}

/** Estado de la red del dispositivo; la implementación es de la plataforma. */
interface MonitorConexion {
    val conectado: Flow<Boolean>
}
