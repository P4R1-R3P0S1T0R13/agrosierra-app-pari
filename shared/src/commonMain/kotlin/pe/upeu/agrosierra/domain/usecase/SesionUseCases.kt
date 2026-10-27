package pe.upeu.agrosierra.domain.usecase

import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Resultado
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.repository.SesionRepository

class IniciarSesionUseCase(private val repositorio: SesionRepository) {
    suspend operator fun invoke(usuario: String, contrasena: String): Resultado<Sesion> {
        val campos = buildMap {
            if (usuario.isBlank()) put("usuario", "Ingrese su usuario")
            if (contrasena.isBlank()) put("contrasena", "Ingrese su contrasena")
        }
        if (campos.isNotEmpty()) return Resultado.Fallo(ErrorApi.Validacion(campos))
        return repositorio.iniciarSesion(usuario.trim(), contrasena)
    }
}

class RestaurarSesionUseCase(private val repositorio: SesionRepository) {
    suspend operator fun invoke(): Sesion? = repositorio.restaurar()
}

class CerrarSesionUseCase(private val repositorio: SesionRepository) {
    suspend operator fun invoke() = repositorio.cerrarSesion()
}
