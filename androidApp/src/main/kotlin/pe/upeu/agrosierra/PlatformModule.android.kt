package pe.upeu.agrosierra

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.upeu.agrosierra.data.local.AlmacenSeguro
import pe.upeu.agrosierra.data.local.FabricaDriver
import pe.upeu.agrosierra.data.remote.ApiConfig
import pe.upeu.agrosierra.domain.repository.MonitorConexion

/** Dependencias propias de Android: motor HTTP, almacén cifrado, driver SQLite y monitor de red. */
val platformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single { ApiConfig(urlBase = BuildConfig.URL_BASE, registroDetallado = BuildConfig.REGISTRO_DETALLADO) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<FabricaDriver> { BaseLocalAndroid(androidContext()) }
    single<MonitorConexion> { MonitorConexionAndroid(androidContext()) }
}
