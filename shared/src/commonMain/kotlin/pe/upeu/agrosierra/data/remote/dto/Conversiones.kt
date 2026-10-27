package pe.upeu.agrosierra.data.remote.dto

import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.model.TipoLabor

/** Funciones de conversión DTO -> dominio: el dominio no conoce anotaciones de serialización. */
fun LoteDto.aDominio() = Lote(
    id = id,
    codigo = codigo,
    nombre = nombre,
    hectareas = hectareas,
    cultivo = cultivo,
    valle = valle,
    campaniaCerrada = campaniaCerrada,
    dosisMaximaPorHectarea = dosisMaxima,
)

fun LaborDto.aDominio() = Labor(
    id = id,
    loteId = loteId,
    tipo = TipoLabor.entries.firstOrNull { it.name == tipo } ?: TipoLabor.RIEGO,
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
    pendiente = false,
)

fun Labor.aPeticion() = LaborRequestDto(
    loteId = loteId,
    tipo = tipo.name,
    fecha = fecha,
    responsable = responsable,
    cantidad = cantidad,
    observacion = observacion,
)

fun LoginResponseDto.aSesion() = Sesion(
    token = token,
    usuario = usuario,
    rol = rol,
    privilegios = privilegios,
    nombre = nombre,
    expiraEn = expiraEn,
)
