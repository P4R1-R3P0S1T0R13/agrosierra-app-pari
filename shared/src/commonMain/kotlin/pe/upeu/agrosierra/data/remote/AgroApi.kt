package pe.upeu.agrosierra.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import pe.upeu.agrosierra.data.remote.dto.LaborDto
import pe.upeu.agrosierra.data.remote.dto.LaborRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoginRequestDto
import pe.upeu.agrosierra.data.remote.dto.LoginResponseDto
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.data.remote.dto.PaginaDto
import pe.upeu.agrosierra.data.remote.dto.UsuarioDto
import pe.upeu.agrosierra.domain.model.Resultado

/** Rutas del contrato 4.3, relativas a la dirección base de [ApiConfig]. */
class AgroApi(private val cliente: HttpClient) {

    suspend fun login(usuario: String, contrasena: String): Resultado<LoginResponseDto> = llamarApi {
        cliente.post("auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(usuario, contrasena))
        }
    }

    suspend fun usuarioActual(): Resultado<UsuarioDto> = llamarApi { cliente.get("usuarios/me") }

    suspend fun lotes(pagina: Int, tamanio: Int, valle: String? = null): Resultado<PaginaDto<LoteDto>> = llamarApi {
        cliente.get("lotes") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
            valle?.let { parameter("valle", it) }
        }
    }

    suspend fun lote(id: Long): Resultado<LoteDto> = llamarApi { cliente.get("lotes/$id") }

    suspend fun labores(loteId: Long, pagina: Int, tamanio: Int): Resultado<PaginaDto<LaborDto>> = llamarApi {
        cliente.get("lotes/$loteId/labores") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }
    }

    suspend fun registrarLabor(peticion: LaborRequestDto): Resultado<LaborDto> = llamarApi {
        cliente.post("labores") {
            contentType(ContentType.Application.Json)
            setBody(peticion)
        }
    }

    suspend fun corregirLabor(id: Long, peticion: LaborRequestDto): Resultado<LaborDto> = llamarApi {
        cliente.put("labores/$id") {
            contentType(ContentType.Application.Json)
            setBody(peticion)
        }
    }

    suspend fun eliminarLabor(id: Long): Resultado<Unit> = llamarApi { cliente.delete("labores/$id") }

    suspend fun cerrarCampania(loteId: Long): Resultado<LoteDto> = llamarApi {
        cliente.post("lotes/$loteId/cerrar-campania")
    }
}
