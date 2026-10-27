package pe.upeu.agrosierra.domain.model

data class Labor(
    val id: Long,              // negativo mientras esté pendiente de envío
    val loteId: Long,
    val tipo: TipoLabor,
    val fecha: String,         // ISO-8601: 2026-10-27
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
    val pendiente: Boolean,    // true mientras no haya llegado al servidor
)
