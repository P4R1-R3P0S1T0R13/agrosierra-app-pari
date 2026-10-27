package pe.upeu.agrosierra.data.remote

import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import pe.upeu.agrosierra.data.remote.dto.ErrorDto
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado

/**
 * Ejecuta una petición y traduce TODO lo que puede pasar a un [Resultado]:
 * códigos de estado, falta de red y tiempo agotado. Es el único lugar que conoce HTTP.
 */
suspend inline fun <reified T> llamarApi(crossinline peticion: suspend () -> HttpResponse): Resultado<T> =
    try {
        val respuesta = peticion()
        if (respuesta.status.isSuccess()) {
            // 204 No Content: no se intenta convertir el cuerpo, solo se comprueba el código.
            if (T::class == Unit::class) Resultado.Exito(Unit as T) else Resultado.Exito(respuesta.body<T>())
        } else {
            Resultado.Fallo(traducirError(respuesta.status.value, leerError(respuesta)))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpRequestTimeoutException) {
        Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: ConnectTimeoutException) {
        Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: SocketTimeoutException) {
        Resultado.Fallo(ErrorApi.TiempoAgotado)
    } catch (e: IOException) {
        Resultado.Fallo(ErrorApi.SinConexion)
    } catch (e: SerializationException) {
        Resultado.Fallo(ErrorApi.Desconocido("Respuesta inesperada del servidor"))
    } catch (e: Exception) {
        Resultado.Fallo(ErrorApi.Desconocido(e.message ?: "Ocurrio un error inesperado"))
    }

suspend fun leerError(respuesta: HttpResponse): ErrorDto? = try {
    jsonApi.decodeFromString(ErrorDto.serializer(), respuesta.bodyAsText())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Traduce el código de estado y el cuerpo de error común (4.2) a un error del dominio. */
fun traducirError(estado: Int, error: ErrorDto?): ErrorApi {
    val mensaje = error?.message?.takeIf { it.isNotBlank() }
    return when (estado) {
        400 -> ErrorApi.Validacion(error?.validationErrors.orEmpty(), mensaje ?: "Revise los datos ingresados")
        401 -> ErrorApi.NoAutorizado()
        403 -> mensaje?.let { ErrorApi.SinPrivilegio(it) } ?: ErrorApi.SinPrivilegio()
        404 -> mensaje?.let { ErrorApi.NoEncontrado(it) } ?: ErrorApi.NoEncontrado()
        409 -> ErrorApi.Conflicto(mensaje ?: "La operacion no cumple una regla del negocio")
        in 500..599 -> ErrorApi.Servidor()
        else -> ErrorApi.Desconocido(mensaje ?: "Respuesta inesperada ($estado)")
    }
}
