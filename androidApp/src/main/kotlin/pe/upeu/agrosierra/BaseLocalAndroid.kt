package pe.upeu.agrosierra

import android.content.Context
import app.cash.sqldelight.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import pe.upeu.agrosierra.data.local.FabricaDriver

/** Driver de SQLDelight para Android: la base vive en el almacenamiento interno de la app. */
class BaseLocalAndroid(private val contexto: Context) : FabricaDriver {
    override fun crear(): SqlDriver = AndroidSqliteDriver(AgroDatabase.Schema, contexto, "agrosierra.db")
}
