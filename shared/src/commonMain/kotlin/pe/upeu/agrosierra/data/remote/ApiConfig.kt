package pe.upeu.agrosierra.data.remote

/**
 * Dirección base centralizada y política de registro. La plataforma la construye
 * (en Android, desde BuildConfig) para no escribir URLs en cada llamada.
 */
data class ApiConfig(
    val urlBase: String,
    val registroDetallado: Boolean,
    val tiempoEsperaMs: Long = 15_000,
)
