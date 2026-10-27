package pe.upeu.agrosierra

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.upeu.agrosierra.data.remote.dto.ErrorDto
import pe.upeu.agrosierra.data.remote.traducirError
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TraduccionErroresTest {

    @Test
    fun cadaCodigoDeEstadoSeTraduceAUnErrorTipadoDistinto() {
        assertIs<ErrorApi.NoAutorizado>(traducirError(401, null))
        assertIs<ErrorApi.SinPrivilegio>(traducirError(403, null))
        assertIs<ErrorApi.NoEncontrado>(traducirError(404, null))
        assertIs<ErrorApi.Servidor>(traducirError(500, null))
        val conflicto = traducirError(409, ErrorDto(status = 409, message = "La campania esta cerrada"))
        assertEquals(ErrorApi.Conflicto("La campania esta cerrada"), conflicto)
    }

    @Test
    fun un400ConservaElMensajeDeCadaCampo() = runTest {
        val api = apiSimulada { _ ->
            json(
                """{"status":400,"error":"Bad Request","message":"Datos invalidos","path":"/api/v1/labores",
                   "validationErrors":{"cantidad":"La dosis supera el maximo del lote"}}""",
                HttpStatusCode.BadRequest,
            )
        }

        val r = api.registrarLabor(laborDePrueba())

        val error = assertIs<ErrorApi.Validacion>(assertIs<Resultado.Fallo>(r).error)
        assertEquals("La dosis supera el maximo del lote", error.campos["cantidad"])
    }

    @Test
    fun sinRedSeTraduceASinConexion() = runTest {
        val api = apiSimulada { _ -> throw IOException("red caida") }

        val r = api.lotes(0, 20)

        assertEquals(ErrorApi.SinConexion, assertIs<Resultado.Fallo>(r).error)
    }

    @Test
    fun laCabeceraSeAgregaEnUnSoloLugarYUn401CierraLaSesion() = runTest {
        var cabecera: String? = null
        var sesionCerrada = false
        val api = apiSimulada(token = "abc", alRecibir401 = { sesionCerrada = true }) { peticion ->
            cabecera = peticion.headers[HttpHeaders.Authorization]
            json("""{"status":401,"message":"El token vencio"}""", HttpStatusCode.Unauthorized)
        }

        val r = api.usuarioActual()

        assertEquals("Bearer abc", cabecera)
        assertIs<ErrorApi.NoAutorizado>(assertIs<Resultado.Fallo>(r).error)
        assertTrue(sesionCerrada)
    }
}
