package pe.upeu.agrosierra.domain.model

/**
 * Jerarquía sellada de errores del dominio. La capa de datos traduce cada respuesta
 * del servicio a uno de estos casos y la interfaz decide una conducta distinta para cada uno.
 */
sealed class ErrorApi(open val mensaje: String) {
    /** 400: validación fallida; [campos] trae el mensaje de cada campo. */
    data class Validacion(
        val campos: Map<String, String>,
        override val mensaje: String = "Revise los datos ingresados",
    ) : ErrorApi(mensaje)

    /** 401: token ausente, alterado o vencido. */
    data class NoAutorizado(override val mensaje: String = "Su sesion termino, ingrese nuevamente") : ErrorApi(mensaje)

    /** 403: autenticado pero sin el privilegio requerido. */
    data class SinPrivilegio(override val mensaje: String = "No esta autorizado para esta operacion") : ErrorApi(mensaje)

    /** 404: el recurso no existe. */
    data class NoEncontrado(override val mensaje: String = "El registro ya no existe") : ErrorApi(mensaje)

    /** 409: regla de negocio del servidor incumplida. */
    data class Conflicto(override val mensaje: String) : ErrorApi(mensaje)

    /** 500: fallo del servidor. */
    data class Servidor(override val mensaje: String = "El servidor tuvo un problema, intente de nuevo") : ErrorApi(mensaje)

    data object SinConexion : ErrorApi("Sin conexion con el servidor")

    data object TiempoAgotado : ErrorApi("El servidor tardo demasiado en responder")

    data class Desconocido(override val mensaje: String = "Ocurrio un error inesperado") : ErrorApi(mensaje)
}
