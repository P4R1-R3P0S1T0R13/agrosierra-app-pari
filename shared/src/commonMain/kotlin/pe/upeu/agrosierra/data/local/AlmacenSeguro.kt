package pe.upeu.agrosierra.data.local

/**
 * Almacenamiento cifrado de clave-valor. En Android lo implementa AlmacenSeguroAndroid con
 * preferencias cifradas cuya clave maestra vive en el almacén de claves (Android Keystore).
 */
interface AlmacenSeguro {
    suspend fun guardar(valores: Map<String, String>)
    suspend fun leer(clave: String): String?
    suspend fun borrarTodo()
}
