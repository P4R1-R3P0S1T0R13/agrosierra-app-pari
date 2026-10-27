package pe.upeu.agrosierra

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.upeu.agrosierra.domain.repository.MonitorConexion

/** Emite true/false según haya una red con acceso a internet; dispara la sincronización automática. */
class MonitorConexionAndroid(contexto: Context) : MonitorConexion {

    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)

    override val conectado: Flow<Boolean> = callbackFlow {
        fun hayRed(): Boolean = gestor.getNetworkCapabilities(gestor.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Durante onLost, activeNetwork aún puede devolver la red perdida: se llevan las redes vivas.
        val redes = mutableSetOf<Network>()
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(redes) { redes.add(network) }
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(synchronized(redes) { redes.remove(network); redes.isNotEmpty() })
            }
        }
        trySend(hayRed())
        val solicitud = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        gestor.registerNetworkCallback(solicitud, escucha)
        awaitClose { gestor.unregisterNetworkCallback(escucha) }
    }.distinctUntilChanged()
}
