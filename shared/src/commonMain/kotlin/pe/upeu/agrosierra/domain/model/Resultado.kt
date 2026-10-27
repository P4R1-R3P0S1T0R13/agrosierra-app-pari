package pe.upeu.agrosierra.domain.model

sealed interface Resultado<out T> {
    data class Exito<T>(val datos: T) : Resultado<T>
    data class Fallo(val error: ErrorApi) : Resultado<Nothing>
}

inline fun <T, R> Resultado<T>.mapear(transformar: (T) -> R): Resultado<R> = when (this) {
    is Resultado.Exito -> Resultado.Exito(transformar(datos))
    is Resultado.Fallo -> this
}
