package pe.upeu.agrosierra.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository
import pe.upeu.agrosierra.domain.repository.LoteRepository

class ObservarLotesUseCase(private val repositorio: LoteRepository) {
    operator fun invoke(valle: String?, texto: String): Flow<List<Lote>> = repositorio.observarLotes(valle, texto.trim())
    fun valles(): Flow<List<String>> = repositorio.observarValles()
    fun ultimaSincronizacion(): Flow<String?> = repositorio.observarUltimaSincronizacion()
}

class ObservarDetalleLoteUseCase(
    private val lotes: LoteRepository,
    private val labores: LaborRepository,
) {
    fun lote(id: Long): Flow<Lote?> = lotes.observarLote(id)
    fun labores(id: Long): Flow<List<Labor>> = labores.observarLabores(id)
}

class CerrarCampaniaUseCase(private val repositorio: LoteRepository) {
    suspend operator fun invoke(loteId: Long): Resultado<Lote> = repositorio.cerrarCampania(loteId)
}
