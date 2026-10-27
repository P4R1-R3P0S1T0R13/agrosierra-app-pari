package pe.upeu.agrosierra.domain.model

/** Nombres de privilegio del contrato (sección 3.3). La interfaz decide qué ofrecer con ellos, nunca con el rol. */
object Privilegios {
    const val LOTE_LEER = "LOTE_LEER"
    const val LABOR_LEER = "LABOR_LEER"
    const val LABOR_CREAR = "LABOR_CREAR"
    const val LABOR_EDITAR = "LABOR_EDITAR"
    const val LABOR_ELIMINAR = "LABOR_ELIMINAR"
    const val CAMPANIA_CERRAR = "CAMPANIA_CERRAR"
    const val USUARIO_GESTIONAR = "USUARIO_GESTIONAR"
}
