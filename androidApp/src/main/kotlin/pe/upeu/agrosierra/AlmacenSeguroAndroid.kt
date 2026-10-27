package pe.upeu.agrosierra

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.upeu.agrosierra.data.local.AlmacenSeguro

/**
 * Preferencias cifradas (AES-256) cuya clave maestra la custodia el Android Keystore:
 * el token no queda legible aunque se extraiga el archivo de preferencias.
 */
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val preferencias: SharedPreferences by lazy {
        val claveMaestra = MasterKey.Builder(contexto)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            contexto,
            ARCHIVO,
            claveMaestra,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun guardar(valores: Map<String, String>) = withContext(Dispatchers.IO) {
        preferencias.edit().apply { valores.forEach { (clave, valor) -> putString(clave, valor) } }.commit()
        Unit
    }

    override suspend fun leer(clave: String): String? = withContext(Dispatchers.IO) {
        preferencias.getString(clave, null)
    }

    override suspend fun borrarTodo() = withContext(Dispatchers.IO) {
        preferencias.edit().clear().commit()
        Unit
    }

    private companion object {
        const val ARCHIVO = "agro_sesion_segura"
    }
}
