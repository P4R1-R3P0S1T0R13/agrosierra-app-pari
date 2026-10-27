package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.data.remote.dto.aPeticion
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository

/**
 * Registro y corrección offline-first: la fila se guarda primero en local marcada como pendiente
 * y después se intenta enviar. Sin red queda pendiente; la sincronización la subirá después.
 */
class LaborRepositoryImpl(
    private val api: AgroApi,
    private val base: BaseLocal,
) : LaborRepository {

    override fun observarLabores(loteId: Long): Flow<List<Labor>> = base.observarLabores(loteId)

    override suspend fun obtener(id: Long): Labor? = base.obtenerLabor(id)

    override suspend fun registrar(labor: Labor): Resultado<Labor> {
        val local = base.insertarLaborPendiente(labor)
        return when (val r = api.registrarLabor(local.aPeticion())) {
            is Resultado.Exito -> {
                val definitiva = r.datos.aDominio()
                base.reemplazarLabor(local.id, definitiva)
                Resultado.Exito(definitiva)
            }
            is Resultado.Fallo -> when {
                // Sin red o sesión vencida: la fila queda pendiente con su id provisional.
                r.error.seConservaPendiente() -> Resultado.Exito(local)
                else -> {
                    // El servicio la rechazó (400/403/409): no se guarda y se muestra el motivo.
                    base.eliminarLabor(local.id)
                    r
                }
            }
        }
    }

    override suspend fun corregir(labor: Labor): Resultado<Labor> {
        // Una labor que nunca llegó al servidor se corrige solo en local y sigue pendiente.
        if (labor.id < 0) {
            val local = labor.copy(pendiente = true)
            base.guardarLabor(local)
            return Resultado.Exito(local)
        }
        return when (val r = api.corregirLabor(labor.id, labor.aPeticion())) {
            is Resultado.Exito -> {
                val definitiva = r.datos.aDominio()
                base.guardarLabor(definitiva)
                Resultado.Exito(definitiva)
            }
            is Resultado.Fallo -> when {
                r.error.seConservaPendiente() -> {
                    val local = labor.copy(pendiente = true)
                    base.guardarLabor(local)
                    Resultado.Exito(local)
                }
                r.error is ErrorApi.NoEncontrado -> {
                    base.eliminarLabor(labor.id)
                    r
                }
                else -> r
            }
        }
    }

    override suspend fun eliminar(id: Long): Resultado<Unit> {
        // Descartar una labor que nunca se envió no requiere al servidor.
        if (id < 0) {
            base.eliminarLabor(id)
            return Resultado.Exito(Unit)
        }
        return when (val r = api.eliminarLabor(id)) {
            is Resultado.Exito -> {
                base.eliminarLabor(id)
                r
            }
            is Resultado.Fallo -> {
                if (r.error is ErrorApi.NoEncontrado) base.eliminarLabor(id)
                r
            }
        }
    }
}

internal fun ErrorApi.seConservaPendiente(): Boolean =
    this is ErrorApi.SinConexion || this is ErrorApi.TiempoAgotado || this is ErrorApi.NoAutorizado
