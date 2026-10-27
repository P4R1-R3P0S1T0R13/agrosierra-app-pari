package pe.upeu.agrosierra

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import pe.upeu.agrosierra.data.local.BaseLocal
import pe.upeu.agrosierra.data.remote.AgroApi
import pe.upeu.agrosierra.data.remote.ApiConfig
import pe.upeu.agrosierra.data.remote.crearClienteHttp

/** Driver de SQLite en memoria; cada plataforma de prueba entrega el suyo. */
expect fun crearDriverDePrueba(): SqlDriver

fun baseEnMemoria() = BaseLocal({ crearDriverDePrueba() })

const val URL_PRUEBA = "http://agroapi.test/api/v1/"

/** AgroApi sobre el motor HTTP simulado; nada sale a la red. */
fun apiSimulada(
    token: String? = "token-prueba",
    alRecibir401: suspend () -> Unit = {},
    manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): AgroApi = AgroApi(
    crearClienteHttp(
        motor = MockEngine(manejador),
        config = ApiConfig(urlBase = URL_PRUEBA, registroDetallado = false),
        leerToken = { token },
        alRecibirNoAutorizado = alRecibir401,
    ),
)

fun MockRequestHandleScope.json(cuerpo: String, estado: HttpStatusCode = HttpStatusCode.OK) =
    respond(cuerpo, estado, headersOf(HttpHeaders.ContentType, "application/json"))
