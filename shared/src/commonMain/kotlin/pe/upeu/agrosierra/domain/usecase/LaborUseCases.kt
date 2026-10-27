package pe.upeu.agrosierra.domain.usecase

import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.repository.LaborRepository

/**
 * Registra o corrige una labor. Valida el formato del formulario antes de guardar;
 * las reglas de negocio (RN-01 a RN-04) las decide el servicio y su motivo se muestra tal cual.
 */
class GuardarLaborUseCase(private val repositorio: LaborRepository) {

    suspend operator fun invoke(labor: Labor): Resultado<Labor> {
        val campos = validar(labor)
        if (campos.isNotEmpty()) return Resultado.Fallo(ErrorApi.Validacion(campos))
        val limpia = labor.copy(responsable = labor.responsable.trim(), observacion = labor.observacion.trim())
        return if (labor.id == NUEVA) repositorio.registrar(limpia) else repositorio.corregir(limpia)
    }

    private fun validar(labor: Labor): Map<String, String> = buildMap {
        if (!FORMATO_FECHA.matches(labor.fecha)) put("fecha", "Use el formato AAAA-MM-DD")
        if (labor.responsable.isBlank()) put("responsable", "Ingrese el responsable")
        if (labor.cantidad <= 0.0) put("cantidad", "La cantidad debe ser mayor que cero")
        if (labor.observacion.length > 500) put("observacion", "Maximo 500 caracteres")
    }

    companion object {
        /** Identificador de una labor que aún no existe ni en local ni en el servidor. */
        const val NUEVA = 0L
        private val FORMATO_FECHA = Regex("""\d{4}-\d{2}-\d{2}""")
    }
}

class ObtenerLaborUseCase(private val repositorio: LaborRepository) {
    suspend operator fun invoke(id: Long): Labor? = repositorio.obtener(id)
}

class EliminarLaborUseCase(private val repositorio: LaborRepository) {
    suspend operator fun invoke(id: Long): Resultado<Unit> = repositorio.eliminar(id)
}
