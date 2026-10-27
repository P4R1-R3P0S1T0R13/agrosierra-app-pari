package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Serializador común: tolera claves desconocidas para que un campo nuevo del servicio no rompa la app. */
val jsonApi = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

class ConfigSesionHttp {
    var leerToken: suspend () -> String? = { null }
    var alRecibirNoAutorizado: suspend () -> Unit = {}
}

/**
 * Punto ÚNICO donde se añade la cabecera Authorization a todas las peticiones
 * y donde un 401 dispara el cierre de la sesión local.
 */
val SesionHttp = createClientPlugin("SesionHttp", ::ConfigSesionHttp) {
    val leerToken = pluginConfig.leerToken
    val alRecibirNoAutorizado = pluginConfig.alRecibirNoAutorizado

    onRequest { peticion, _ ->
        if (!peticion.url.buildString().contains(RUTA_LOGIN)) {
            leerToken()?.let { peticion.headers[HttpHeaders.Authorization] = "Bearer $it" }
        }
    }
    onResponse { respuesta ->
        val esLogin = respuesta.call.request.url.encodedPath.contains(RUTA_LOGIN)
        if (respuesta.status == HttpStatusCode.Unauthorized && !esLogin) alRecibirNoAutorizado()
    }
}

private const val RUTA_LOGIN = "auth/login"

/** Única instancia de HttpClient de la app; Koin la crea una sola vez con el motor de la plataforma. */
fun crearClienteHttp(
    motor: HttpClientEngine,
    config: ApiConfig,
    leerToken: suspend () -> String?,
    alRecibirNoAutorizado: suspend () -> Unit,
): HttpClient = HttpClient(motor) {
    expectSuccess = false

    install(ContentNegotiation) { json(jsonApi) }

    install(HttpTimeout) {
        requestTimeoutMillis = config.tiempoEsperaMs
        connectTimeoutMillis = config.tiempoEsperaMs
        socketTimeoutMillis = config.tiempoEsperaMs
    }

    install(Logging) {
        logger = Logger.SIMPLE
        // Registro reducido en producción: nunca se escriben cuerpos ni el token.
        level = if (config.registroDetallado) LogLevel.INFO else LogLevel.NONE
        sanitizeHeader { it == HttpHeaders.Authorization }
    }

    install(SesionHttp) {
        this.leerToken = leerToken
        this.alRecibirNoAutorizado = alRecibirNoAutorizado
    }

    defaultRequest { url(config.urlBase) }
}
