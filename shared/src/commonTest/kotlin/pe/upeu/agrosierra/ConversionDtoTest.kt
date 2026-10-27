package pe.upeu.agrosierra

import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.data.remote.dto.PaginaDto
import pe.upeu.agrosierra.data.remote.dto.aDominio
import pe.upeu.agrosierra.data.remote.jsonApi
import pe.upeu.agrosierra.domain.model.TipoLabor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversionDtoTest {

    @Test
    fun loteDtoSeConvierteAlDominioUsandoElNombreSerializadoEIgnorandoClavesDesconocidas() {
        val cuerpo = """
            {"id":2,"codigo":"LT-02","nombre":"Santa Rosa","hectareas":1.8,"cultivo":"Maiz","valle":"Mantaro",
             "campaniaCerrada":false,"estadoCampania":"ABIERTA","dosisMaximaPorHectarea":3.5,"campoNuevo":1}
        """.trimIndent()

        val lote = jsonApi.decodeFromString(LoteDto.serializer(), cuerpo).aDominio()

        assertEquals("LT-02", lote.codigo)
        assertEquals(3.5, lote.dosisMaximaPorHectarea)
        assertFalse(lote.campaniaCerrada)
    }

    @Test
    fun listadoSeLeeDesdeElEnvoltorioDePaginacion() {
        val cuerpo = """
            {"contenido":[{"id":7,"loteId":2,"tipo":"FUMIGACION","fecha":"2026-10-27","responsable":"Juan",
              "cantidad":5.0,"observacion":"ok","registradoPor":"tecnico1"}],
             "pagina":0,"tamanio":20,"totalElementos":1,"totalPaginas":1,"ultima":true}
        """.trimIndent()

        val pagina = jsonApi.decodeFromString(PaginaDto.serializer(LaborDto.serializer()), cuerpo)
        val labor = pagina.contenido.single().aDominio()

        assertTrue(pagina.ultima)
        assertEquals(TipoLabor.FUMIGACION, labor.tipo)
        assertEquals(7L, labor.id)
        assertFalse(labor.pendiente, "Lo que llega del servidor nunca esta pendiente")
    }
}
