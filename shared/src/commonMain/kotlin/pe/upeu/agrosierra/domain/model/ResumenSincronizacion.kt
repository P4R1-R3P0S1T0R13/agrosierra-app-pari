package pe.upeu.agrosierra.domain.model

/** Lo que dejó una sincronización: lo enviado, lo recibido y lo que el servidor rechazó. */
data class ResumenSincronizacion(
    val enviadas: Int,
    val lotesRecibidos: Int,
    val laboresRecibidas: Int,
    val rechazadas: List<String> = emptyList(),
)

/** Resultado de la fase de envío; [errorFatal] corta la sincronización (sin red, sesión vencida). */
data class ResumenEnvio(
    val enviadas: Int,
    val rechazadas: List<String>,
    val errorFatal: ErrorApi? = null,
)

data class ResumenDescarga(val lotes: Int, val labores: Int)
