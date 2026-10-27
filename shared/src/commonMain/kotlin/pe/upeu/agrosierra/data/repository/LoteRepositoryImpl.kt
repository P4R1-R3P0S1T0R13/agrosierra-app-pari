package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LoteRepository

/** Offline-first: la lectura SIEMPRE sale de la base local; la red solo la actualiza. */
class LoteRepositoryImpl(
    private val api: AgroApi,
    private val base: BaseLocal,
) : LoteRepository {

    override fun observarLotes(valle: String?, texto: String): Flow<List<Lote>> = base.observarLotes(valle, texto)

    override fun observarValles(): Flow<List<String>> = base.observarValles()

    override fun observarLote(id: Long): Flow<Lote?> = base.observarLote(id)

    override fun observarUltimaSincronizacion(): Flow<String?> = base.observarMetadato(BaseLocal.ULTIMA_SINCRONIZACION)

    /** Cerrar campaña exige conexión: la regla RN-03 solo la puede decidir el servicio. */
    override suspend fun cerrarCampania(id: Long): Resultado<Lote> =
        when (val r = api.cerrarCampania(id)) {
            is Resultado.Exito -> {
                val lote = r.datos.aDominio()
                base.guardarLote(lote)
                Resultado.Exito(lote)
            }
            is Resultado.Fallo -> r
        }
}
