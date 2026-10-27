package pe.upeu.agrosierra.domain.model

data class Sesion(
    val token: String,
    val usuario: String,
    val rol: String,
    val privilegios: Set<String>,
    val nombre: String = usuario,
    /** Instante de vencimiento del token en milisegundos desde epoch. */
    val expiraEn: Long = 0,
) {
    fun tiene(privilegio: String): Boolean = privilegio in privilegios
}
