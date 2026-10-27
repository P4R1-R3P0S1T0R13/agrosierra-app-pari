package pe.upeu.agrosierra.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.ResumenSincronizacion
import pe.upeu.agrosierra.domain.repository.SincronizacionRepository

/**
 * Sincronización offline-first: PRIMERO sube lo pendiente y DESPUÉS baja lo actualizado.
 * Si se invirtiera el orden, la descarga podría pisar filas locales que aún no llegaron al servidor.
 */
class SincronizarUseCase(private val repositorio: SincronizacionRepository) {

    enum class Etapa { ENVIANDO, DESCARGANDO }

    suspend operator fun invoke(alAvanzar: (Etapa) -> Unit = {}): Resultado<ResumenSincronizacion> {
        alAvanzar(Etapa.ENVIANDO)
        val envio = repositorio.enviarPendientes()
        envio.errorFatal?.let { return Resultado.Fallo(it) }

        alAvanzar(Etapa.DESCARGANDO)
        return when (val descarga = repositorio.descargarCambios()) {
            is Resultado.Exito -> Resultado.Exito(
                ResumenSincronizacion(
                    enviadas = envio.enviadas,
                    lotesRecibidos = descarga.datos.lotes,
                    laboresRecibidas = descarga.datos.labores,
                    rechazadas = envio.rechazadas,
                ),
            )
            is Resultado.Fallo -> descarga
        }
    }

    fun pendientes(): Flow<Long> = repositorio.observarPendientes()
}
