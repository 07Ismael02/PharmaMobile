package pe.edu.upeu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.edu.upeu.domain.presentation.cliente.ClienteScreen
import pe.edu.upeu.domain.presentation.acerca.AcercaDeScreen
import pe.edu.upeu.domain.presentation.producto.ProductoScreen
import pe.edu.upeu.domain.presentation.auth.LoginScreen
import pe.edu.upeu.domain.presentation.auth.SesionViewModel
import pe.edu.upeu.navigation.Screen
import pe.edu.upeu.domain.presentation.inicio.InicioScreen
import pe.edu.upeu.theme.PharmaMobilTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val sesionViewModel: SesionViewModel = koinViewModel()
    val estadoSesion by sesionViewModel.estado.collectAsStateWithLifecycle()
    val procesando by sesionViewModel.procesando.collectAsStateWithLifecycle()
    val permisos = estadoSesion.sesion?.permisos.orEmpty()
    val puedeEditar = "CATALOGO_ESCRIBIR" in permisos
    var pantallaActual by remember {
        mutableStateOf<Screen>(Screen.Inicio)
    }
    LaunchedEffect(estadoSesion.sesion?.usuario) { pantallaActual = Screen.Inicio }

    var darkTheme by remember {
        mutableStateOf(false)
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()

    PharmaMobilTheme(darkTheme = darkTheme) {
        if (estadoSesion.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (estadoSesion.restauracionPendiente) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estadoSesion.mensaje ?: "No se pudo comprobar la sesión.")
                Button(onClick = sesionViewModel::restaurar) { Text("Reintentar") }
                TextButton(onClick = sesionViewModel::cerrar) { Text("Cerrar sesión") }
            }
        } else if (estadoSesion.sesion == null) {
            LoginScreen(estadoSesion.mensaje, procesando, sesionViewModel::iniciar)
        } else {
        BoxWithConstraints {
            when {
                maxWidth < 600.dp -> {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                DrawerContent(
                                    pantallaActual = pantallaActual,
                                    onScreenSelected = { screen ->
                                        pantallaActual = screen
                                        scope.launch { drawerState.close() }
                                    },
                                    darkTheme = darkTheme,
                                    onDarkThemeChange = { darkTheme = it },
                                    permisos = permisos,
                                    onCerrarSesion = sesionViewModel::cerrar
                                )
                            }
                        }
                    ) {
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            },
                            puedeEditar = puedeEditar,
                            generacionSesion = estadoSesion.generacion
                        )
                    }
                }

                maxWidth < 840.dp -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppNavigationRail(
                            pantallaActual = pantallaActual,
                            onScreenSelected = { pantallaActual = it },
                            darkTheme = darkTheme,
                            onDarkThemeChange = { darkTheme = it },
                            permisos = permisos,
                            onCerrarSesion = sesionViewModel::cerrar
                        )
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = null,
                            modifier = Modifier.weight(1f),
                            puedeEditar = puedeEditar,
                            generacionSesion = estadoSesion.generacion
                        )
                    }
                }

                else -> {
                    PermanentNavigationDrawer(
                        drawerContent = {
                            PermanentDrawerSheet {
                                DrawerContent(
                                    pantallaActual = pantallaActual,
                                    onScreenSelected = { pantallaActual = it },
                                    darkTheme = darkTheme,
                                    onDarkThemeChange = { darkTheme = it },
                                    permisos = permisos,
                                    onCerrarSesion = sesionViewModel::cerrar
                                )
                            }
                        }
                    ) {
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = null,
                            puedeEditar = puedeEditar,
                            generacionSesion = estadoSesion.generacion
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun DrawerContent(
    pantallaActual: Screen,
    onScreenSelected: (Screen) -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    permisos: Set<String>,
    onCerrarSesion: () -> Unit
) {
    DrawerHeader()

    DrawerItem(
        label = "Inicio",
        selected = pantallaActual is Screen.Inicio,
        onClick = { onScreenSelected(Screen.Inicio) },
        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") }
    )
    DrawerItem(
        label = "Productos",
        selected = pantallaActual is Screen.Productos,
        onClick = { onScreenSelected(Screen.Productos) },
        icon = { Icon(Icons.Default.Medication, contentDescription = "Productos") }
    )
    if ("CLIENTES_LEER" in permisos) DrawerItem(
        label = "Clientes",
        selected = pantallaActual is Screen.Clientes,
        onClick = { onScreenSelected(Screen.Clientes) },
        icon = { Icon(Icons.Default.Person, contentDescription = "Clientes") }
    )
    if ("VENTAS_LEER" in permisos) DrawerItem(
        label = "Pedidos",
        selected = pantallaActual is Screen.Pedidos,
        onClick = { onScreenSelected(Screen.Pedidos) },
        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pedidos") }
    )
    DrawerItem(
        label = "Acerca de",
        selected = pantallaActual is Screen.AcercaDe,
        onClick = { onScreenSelected(Screen.AcercaDe) },
        icon = { Icon(Icons.Default.Info, contentDescription = "Acerca de") }
    )

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onCerrarSesion) { Text("Cerrar sesión") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Modo oscuro")
        Switch(
            checked = darkTheme,
            onCheckedChange = onDarkThemeChange
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = icon
    )
}

@Composable
private fun AppNavigationRail(
    pantallaActual: Screen,
    onScreenSelected: (Screen) -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    permisos: Set<String>,
    onCerrarSesion: () -> Unit
) {
    NavigationRail(
        header = {
            Text(
                text = "PM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        RailItem(
            selected = pantallaActual is Screen.Inicio,
            onClick = { onScreenSelected(Screen.Inicio) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = "Inicio"
        )
        RailItem(
            selected = pantallaActual is Screen.Productos,
            onClick = { onScreenSelected(Screen.Productos) },
            icon = { Icon(Icons.Default.Medication, contentDescription = "Productos") },
            label = "Productos"
        )
        if ("CLIENTES_LEER" in permisos) RailItem(
            selected = pantallaActual is Screen.Clientes,
            onClick = { onScreenSelected(Screen.Clientes) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Clientes") },
            label = "Clientes"
        )
        if ("VENTAS_LEER" in permisos) RailItem(
            selected = pantallaActual is Screen.Pedidos,
            onClick = { onScreenSelected(Screen.Pedidos) },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pedidos") },
            label = "Pedidos"
        )
        RailItem(
            selected = pantallaActual is Screen.AcercaDe,
            onClick = { onScreenSelected(Screen.AcercaDe) },
            icon = { Icon(Icons.Default.Info, contentDescription = "Acerca de") },
            label = "Acerca de"
        )

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onCerrarSesion) { Text("Salir") }
        Text(
            text = "Oscuro",
            style = MaterialTheme.typography.labelLarge
        )
        Switch(
            checked = darkTheme,
            onCheckedChange = onDarkThemeChange
        )
    }
}

@Composable
private fun RailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = { Text(label) }
    )
}

@Composable
private fun AppScaffold(
    pantallaActual: Screen,
    onMenuClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    puedeEditar: Boolean,
    generacionSesion: Long
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(tituloPantalla(pantallaActual)) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (pantallaActual) {
                Screen.Inicio -> InicioScreen()
                Screen.Productos -> ProductoScreen(
                    viewModel = koinViewModel(key = "producto-sesion-$generacionSesion"),
                    puedeEditar = puedeEditar
                )
                Screen.Clientes -> ClienteScreen()
                Screen.Pedidos -> Text(
                    text = "Pantalla de pedidos en construcción",
                    modifier = Modifier.padding(16.dp)
                )
                Screen.AcercaDe -> AcercaDeScreen()
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "PharmaMobil",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Gestión farmacéutica",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun tituloPantalla(screen: Screen): String {
    return when (screen) {
        Screen.Inicio -> "Inicio"
        Screen.Productos -> "Productos"
        Screen.Clientes -> "Clientes"
        Screen.Pedidos -> "Pedidos"
        Screen.AcercaDe -> "Acerca de"
    }
}
