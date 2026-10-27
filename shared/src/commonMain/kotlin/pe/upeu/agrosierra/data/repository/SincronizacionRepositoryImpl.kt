package pe.upeu.agrosierra.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.data.remote.dto.PaginaDto
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.data.remote.dto.aPeticion
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.ResumenDescarga
import pe.upeu.agrosierra.domain.model.ResumenEnvio
import pe.upeu.agrosierra.domain.repository.SincronizacionRepository

class SincronizacionRepositoryImpl(
    private val api: AgroApi,
    private val base: BaseLocal,
    private val marcaDeTiempo: () -> String = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString().substringBefore('.')
    },
) : SincronizacionRepository {

    override fun observarPendientes(): Flow<Long> = base.observarPendientes()

    /**
     * Sube cada fila pendiente: las nuevas (id negativo) con POST y las correcciones con PUT.
     * Al confirmarse, la fila provisional se reemplaza por la definitiva en una transacción,
     * así no se duplica ni se pierde. Sin red o con sesión vencida se detiene sin perder nada.
     */
    override suspend fun enviarPendientes(): ResumenEnvio {
        var enviadas = 0
        val rechazadas = mutableListOf<String>()
        for (labor in base.laboresPendientes()) {
            val r = if (labor.id < 0) {
                api.registrarLabor(labor.aPeticion())
            } else {
                api.corregirLabor(labor.id, labor.aPeticion())
            }
            when (r) {
                is Resultado.Exito -> {
                    base.reemplazarLabor(labor.id, r.datos.aDominio())
                    enviadas++
                }
                is Resultado.Fallo -> {
                    if (r.error.seConservaPendiente()) return ResumenEnvio(enviadas, rechazadas, r.error)
                    if (r.error is ErrorApi.NoEncontrado) base.eliminarLabor(labor.id)
                    // Rechazada por una regla del servidor: queda pendiente para corregirla o descartarla.
                    rechazadas += "${labor.tipo.name} del ${labor.fecha}: ${r.error.detalle()}"
                }
            }
        }
        return ResumenEnvio(enviadas, rechazadas)
    }

    /** Baja el padrón y las labores de cada lote recorriendo todas las páginas. */
    override suspend fun descargarCambios(): Resultado<ResumenDescarga> {
        val lotes = when (val r = todasLasPaginas { pagina -> api.lotes(pagina, TAMANIO) }) {
            is Resultado.Exito -> r.datos.map(LoteDto::aDominio)
            is Resultado.Fallo -> return r
        }
        base.guardarLotes(lotes)

        var labores = 0
        for (lote in lotes) {
            val delLote = when (val r = todasLasPaginas { pagina -> api.labores(lote.id, pagina, TAMANIO) }) {
                is Resultado.Exito -> r.datos.map(LaborDto::aDominio)
                is Resultado.Fallo -> return r
            }
            base.reemplazarLaboresDeLote(lote.id, delLote)
            labores += delLote.size
        }
        base.guardarMetadato(BaseLocal.ULTIMA_SINCRONIZACION, marcaDeTiempo())
        return Resultado.Exito(ResumenDescarga(lotes.size, labores))
    }

    private suspend fun <T> todasLasPaginas(pedir: suspend (Int) -> Resultado<PaginaDto<T>>): Resultado<List<T>> {
        val acumulado = mutableListOf<T>()
        var pagina = 0
        while (true) {
            when (val r = pedir(pagina)) {
                is Resultado.Exito -> {
                    acumulado += r.datos.contenido
                    if (r.datos.ultima || r.datos.contenido.isEmpty()) return Resultado.Exito(acumulado)
                    pagina++
                }
                is Resultado.Fallo -> return r
            }
        }
    }

    private fun ErrorApi.detalle(): String =
        if (this is ErrorApi.Validacion && campos.isNotEmpty()) campos.values.joinToString("; ") else mensaje

    private companion object {
        const val TAMANIO = 50
    }
}
