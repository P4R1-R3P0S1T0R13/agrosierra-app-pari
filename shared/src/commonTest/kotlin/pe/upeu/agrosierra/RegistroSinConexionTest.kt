package pe.upeu.agrosierra

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.upeu.agrosierra.data.remote.dto.LaborRequestDto
import pe.upeu.agrosierra.data.repository.LaborRepositoryImpl
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.TipoLabor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

fun laborDePrueba() = LaborRequestDto(2, "RIEGO", "2026-10-27", "Juan Quispe", 50.0, "")

fun laborNueva(loteId: Long = 2) = Labor(0, loteId, TipoLabor.RIEGO, "2026-10-27", "Juan Quispe", 50.0, "", false)

class RegistroSinConexionTest {

    @Test
    fun sinRedLaLaborQuedaGuardadaPendienteYConIdProvisionalNegativo() = runTest {
        val base = baseEnMemoria()
        val repositorio = LaborRepositoryImpl(apiSimulada { _ -> throw IOException("modo avion") }, base)

        val primera = assertIs<Resultado.Exito<Labor>>(repositorio.registrar(laborNueva())).datos
        val segunda = assertIs<Resultado.Exito<Labor>>(repositorio.registrar(laborNueva())).datos

        assertTrue(primera.pendiente)
        assertEquals(-1L, primera.id)
        assertEquals(-2L, segunda.id, "Cada nueva fila toma un id negativo que el servidor nunca emite")
        val enLocal = base.observarLabores(2).first()
        assertEquals(2, enLocal.size)
        assertTrue(enLocal.all { it.pendiente })
    }
}
