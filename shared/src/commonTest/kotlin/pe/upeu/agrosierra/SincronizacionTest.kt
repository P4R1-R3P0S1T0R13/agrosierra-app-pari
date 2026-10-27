package pe.upeu.agrosierra

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.agrosierra.data.repository.SincronizacionRepositoryImpl
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.ResumenDescarga
import pe.upeu.agrosierra.domain.model.ResumenEnvio
import pe.upeu.agrosierra.domain.model.ResumenSincronizacion
import pe.upeu.agrosierra.domain.repository.SincronizacionRepository
import pe.upeu.agrosierra.domain.usecase.SincronizarUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SincronizacionTest {

    @Test
    fun primeroSubeLoPendienteYDespuesBajaSinDuplicarNiPerderFilas() = runTest {
        val base = baseEnMemoria()
        val pendiente = base.insertarLaborPendiente(laborNueva(loteId = 2))
        val peticiones = mutableListOf<String>()

        val api = apiSimulada { peticion ->
            val ruta = peticion.url.encodedPath.removePrefix("/api/v1/")
            peticiones += "${peticion.method.value} $ruta"
            when {
                peticion.method == HttpMethod.Post && ruta == "labores" -> json(
                    """{"id":500,"loteId":2,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Juan Quispe",
                       "cantidad":50.0,"observacion":""}""",
                    HttpStatusCode.Created,
                )
                ruta == "lotes" -> json(
                    """{"contenido":[{"id":2,"codigo":"LT-02","nombre":"Santa Rosa","hectareas":1.8,"cultivo":"Maiz",
                       "valle":"Mantaro","campaniaCerrada":false,"dosisMaximaPorHectarea":3.5}],
                       "pagina":0,"tamanio":50,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                )
                ruta == "lotes/2/labores" -> json(
                    """{"contenido":[
                         {"id":500,"loteId":2,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Juan Quispe","cantidad":50.0},
                         {"id":4,"loteId":2,"tipo":"RIEGO","fecha":"2026-10-18","responsable":"Rosa","cantidad":90.0}],
                       "pagina":0,"tamanio":50,"totalElementos":2,"totalPaginas":1,"ultima":true}""",
                )
                else -> json("""{"status":404}""", HttpStatusCode.NotFound)
            }
        }
        val sincronizar = SincronizarUseCase(SincronizacionRepositoryImpl(api, base, marcaDeTiempo = { "2026-10-27T10:00" }))

        val resumen = assertIs<Resultado.Exito<ResumenSincronizacion>>(sincronizar()).datos

        assertEquals(listOf("POST labores", "GET lotes", "GET lotes/2/labores"), peticiones)
        assertEquals(1, resumen.enviadas)
        val labores = base.observarLabores(2).first()
        assertEquals(listOf(500L, 4L), labores.map { it.id }, "La provisional -1 fue reemplazada por la 500, sin duplicados")
        assertFalse(labores.any { it.pendiente })
        assertEquals(-1L, pendiente.id)
        assertEquals("2026-10-27T10:00", base.observarMetadato("ultima_sincronizacion").first())
    }

    @Test
    fun siNoHayRedAlEnviarNoSeDescargaNada() = runTest {
        val llamadas = mutableListOf<String>()
        val repositorio = object : SincronizacionRepository {
            override fun observarPendientes() = kotlinx.coroutines.flow.flowOf(1L)
            override suspend fun enviarPendientes(): ResumenEnvio {
                llamadas += "enviar"
                return ResumenEnvio(0, emptyList(), ErrorApi.SinConexion)
            }
            override suspend fun descargarCambios(): Resultado<ResumenDescarga> {
                llamadas += "descargar"
                return Resultado.Exito(ResumenDescarga(0, 0))
            }
        }

        val r = SincronizarUseCase(repositorio)()

        assertEquals(ErrorApi.SinConexion, assertIs<Resultado.Fallo>(r).error)
        assertEquals(listOf("enviar"), llamadas)
    }
}
