package pe.upeu.agrosierra.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val usuario: String, val contrasena: String)

@Serializable
data class LoginResponseDto(
    val token: String,
    val expiraEn: Long = 0,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String> = emptySet(),
)

@Serializable
data class UsuarioDto(
    val id: Long,
    val usuario: String,
    val nombre: String,
    val rol: String,
    val privilegios: Set<String> = emptySet(),
)

/** Envoltorio de paginación del servicio: por eso un listado no se deserializa como List<T>. */
@Serializable
data class PaginaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean,
)

@Serializable
data class ErrorDto(
    val timestamp: String? = null,
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val validationErrors: Map<String, String>? = null,
)

@Serializable
data class LoteDto(
    val id: Long,
    val codigo: String,
    val nombre: String,
    val hectareas: Double,
    val cultivo: String,
    val valle: String,
    val campaniaCerrada: Boolean,
    @SerialName("dosisMaximaPorHectarea")
    val dosisMaxima: Double,
)

@Serializable
data class LaborDto(
    val id: Long,
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String = "",
    val registradoPor: String? = null,
)

@Serializable
data class LaborRequestDto(
    val loteId: Long,
    val tipo: String,
    val fecha: String,
    val responsable: String,
    val cantidad: Double,
    val observacion: String,
)
