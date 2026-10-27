package pe.upeu.agrosierra.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.upeu.agrosierra.AgroDatabase
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.TipoLabor
import pe.upeu.agrosierra.Labor as FilaLabor
import pe.upeu.agrosierra.Lote as FilaLote

/** Fábrica del driver de SQLDelight; cada plataforma entrega el suyo (Android: BaseLocalAndroid). */
fun interface FabricaDriver {
    fun crear(): SqlDriver
}

/**
 * Fuente de verdad de la interfaz: todas las pantallas observan estas consultas como Flow
 * y se actualizan solas cuando cambia una fila, haya red o no.
 */
class BaseLocal(
    fabrica: FabricaDriver,
    private val io: CoroutineDispatcher = Dispatchers.Default,
) {
    private val db = AgroDatabase(fabrica.crear())
    private val consultas = db.agroDbQueries

    // ---------- Lotes ----------

    fun observarLotes(valle: String?, texto: String): Flow<List<Lote>> =
        consultas.observarLotes(valle = valle ?: "%", texto = "%$texto%")
            .asFlow().mapToList(io).map { filas -> filas.map { it.aDominio() } }

    fun observarValles(): Flow<List<String>> = consultas.observarValles().asFlow().mapToList(io)

    fun observarLote(id: Long): Flow<Lote?> =
        consultas.lotePorId(id).asFlow().mapToOneOrNull(io).map { it?.aDominio() }

    suspend fun guardarLotes(lotes: List<Lote>) = withContext(io) {
        consultas.transaction { lotes.forEach(::insertarLote) }
    }

    suspend fun guardarLote(lote: Lote) = withContext(io) { insertarLote(lote) }

    private fun insertarLote(l: Lote) = consultas.guardarLote(
        l.id, l.codigo, l.nombre, l.hectareas, l.cultivo, l.valle, if (l.campaniaCerrada) 1 else 0, l.dosisMaximaPorHectarea,
    )

    // ---------- Labores ----------

    fun observarLabores(loteId: Long): Flow<List<Labor>> =
        consultas.observarLaboresPorLote(loteId).asFlow().mapToList(io).map { filas -> filas.map { it.aDominio() } }

    suspend fun obtenerLabor(id: Long): Labor? = withContext(io) {
        consultas.laborPorId(id).executeAsOneOrNull()?.aDominio()
    }

    /** Guarda una labor nueva con un id provisional negativo y la marca como pendiente. */
    suspend fun insertarLaborPendiente(labor: Labor): Labor = withContext(io) {
        consultas.transactionWithResult {
            val menor = consultas.siguienteIdLocal().executeAsOne()
            val local = labor.copy(id = minOf(menor, 0L) - 1, pendiente = true)
            insertarLabor(local)
            local
        }
    }

    suspend fun guardarLabor(labor: Labor) = withContext(io) { insertarLabor(labor) }

    suspend fun eliminarLabor(id: Long) = withContext(io) { consultas.eliminarLabor(id) }

    /** Sustituye la fila provisional por la definitiva del servidor dentro de la misma transacción. */
    suspend fun reemplazarLabor(idAnterior: Long, definitiva: Labor) = withContext(io) {
        consultas.transaction {
            consultas.eliminarLabor(idAnterior)
            insertarLabor(definitiva.copy(pendiente = false))
        }
    }

    suspend fun laboresPendientes(): List<Labor> = withContext(io) {
        consultas.laboresPendientes().executeAsList().map { it.aDominio() }
    }

    fun observarPendientes(): Flow<Long> = consultas.contarPendientes().asFlow().mapToOne(io)

    /**
     * Reemplaza las labores ya sincronizadas de un lote por las del servidor,
     * sin tocar las pendientes (ni las nuevas ni las correcciones aún no enviadas).
     */
    suspend fun reemplazarLaboresDeLote(loteId: Long, delServidor: List<Labor>) = withContext(io) {
        consultas.transaction {
            val pendientes = consultas.laboresPendientes().executeAsList().map { it.id }.toSet()
            consultas.eliminarSincronizadasDeLote(loteId)
            delServidor.filter { it.id !in pendientes }.forEach { insertarLabor(it.copy(pendiente = false)) }
        }
    }

    private fun insertarLabor(l: Labor) = consultas.guardarLabor(
        l.id, l.loteId, l.tipo.name, l.fecha, l.responsable, l.cantidad, l.observacion, if (l.pendiente) 1 else 0,
    )

    // ---------- Metadatos ----------

    fun observarMetadato(clave: String): Flow<String?> =
        consultas.observarMetadato(clave).asFlow().mapToOneOrNull(io)

    suspend fun guardarMetadato(clave: String, valor: String) = withContext(io) {
        consultas.guardarMetadato(clave, valor)
    }

    companion object {
        const val ULTIMA_SINCRONIZACION = "ultima_sincronizacion"
    }
}

private fun FilaLote.aDominio() = Lote(
    id = id,
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = campaniaCerrada == 1L,
    dosisMaximaPorHectarea = dosisMaxima,
)

private fun FilaLabor.aDominio() = Labor(
    id = id,
    loteId = loteId,
    tipo = TipoLabor.entries.firstOrNull { it.name == tipo } ?: TipoLabor.RIEGO,
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = pendiente == 1L,
)
