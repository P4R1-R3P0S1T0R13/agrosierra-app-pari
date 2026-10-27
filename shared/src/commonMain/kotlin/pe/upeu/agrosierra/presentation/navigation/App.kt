package pe.upeu.agrosierra.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.upeu.agrosierra.presentation.comun.EstadoCargando
import pe.upeu.agrosierra.presentation.detalle.DetalleScreen
import pe.upeu.agrosierra.presentation.detalle.DetalleViewModel
import pe.upeu.agrosierra.presentation.detalle.FormularioLaborScreen
import pe.upeu.agrosierra.presentation.detalle.FormularioLaborViewModel
import pe.upeu.agrosierra.presentation.listado.ListadoScreen
import pe.upeu.agrosierra.presentation.perfil.PerfilScreen
import pe.upeu.agrosierra.presentation.sesion.LoginScreen
import pe.upeu.agrosierra.presentation.theme.AgroTheme

@Composable
fun App() {
    AgroTheme {
        val appViewModel: AppViewModel = koinViewModel()
        val estado by appViewModel.estado.collectAsState()
        when (estado) {
            EstadoApp.INICIANDO -> EstadoCargando()
            EstadoApp.SIN_SESION -> LoginScreen()
            EstadoApp.CON_SESION -> AreaProtegida()
        }
    }
}

@Composable
private fun AreaProtegida(navegador: Navegador = koinInject()) {
    val pila by navegador.pila.collectAsState()
    val aviso by navegador.aviso.collectAsState()
    val entrada = pila.last()

    when (val ruta = entrada.ruta) {
        Ruta.Padron -> ListadoScreen(
            alAbrirLote = { navegador.ir(Ruta.Detalle(it)) },
            alAbrirPerfil = { navegador.ir(Ruta.Perfil) },
        )
        is Ruta.Detalle -> DetalleScreen(
            viewModel = koinViewModel<DetalleViewModel>(key = "detalle-${entrada.id}") { parametersOf(ruta.loteId) },
            aviso = aviso,
            alConsumirAviso = navegador::consumirAviso,
            alVolver = { navegador.atras() },
            alRegistrar = { navegador.ir(Ruta.Formulario(ruta.loteId)) },
            alEditar = { navegador.ir(Ruta.Formulario(ruta.loteId, it.id)) },
        )
        is Ruta.Formulario -> FormularioLaborScreen(
            viewModel = koinViewModel<FormularioLaborViewModel>(key = "formulario-${entrada.id}") {
                parametersOf(ruta.loteId, ruta.laborId)
            },
            alTerminar = { navegador.atras(aviso = it) },
            alVolver = { navegador.atras() },
        )
        Ruta.Perfil -> PerfilScreen(alVolver = { navegador.atras() })
    }
}
