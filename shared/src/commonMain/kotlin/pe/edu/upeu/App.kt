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
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.edu.upeu.domain.presentation.cliente.ClienteScreen
import pe.edu.upeu.domain.presentation.producto.ProductoScreen
import pe.edu.upeu.navigation.Screen
import pe.edu.upeu.domain.presentation.inicio.InicioScreen
import pe.edu.upeu.theme.PharmaMobilTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var pantallaActual by remember {
        mutableStateOf<Screen>(Screen.Inicio)
    }

    var darkTheme by remember {
        mutableStateOf(false)
    }

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )
    val scope = rememberCoroutineScope()

    PharmaMobilTheme(darkTheme = darkTheme) {
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
                                    onDarkThemeChange = { darkTheme = it }
                                )
                            }
                        }
                    ) {
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }
                }

                maxWidth < 840.dp -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppNavigationRail(
                            pantallaActual = pantallaActual,
                            onScreenSelected = { pantallaActual = it },
                            darkTheme = darkTheme,
                            onDarkThemeChange = { darkTheme = it }
                        )
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = null,
                            modifier = Modifier.weight(1f)
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
                                    onDarkThemeChange = { darkTheme = it }
                                )
                            }
                        }
                    ) {
                        AppScaffold(
                            pantallaActual = pantallaActual,
                            onMenuClick = null
                        )
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
    onDarkThemeChange: (Boolean) -> Unit
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
    DrawerItem(
        label = "Clientes",
        selected = pantallaActual is Screen.Clientes,
        onClick = { onScreenSelected(Screen.Clientes) },
        icon = { Icon(Icons.Default.Person, contentDescription = "Clientes") }
    )
    DrawerItem(
        label = "Pedidos",
        selected = pantallaActual is Screen.Pedidos,
        onClick = { onScreenSelected(Screen.Pedidos) },
        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pedidos") }
    )

    Spacer(modifier = Modifier.height(8.dp))

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
    onDarkThemeChange: (Boolean) -> Unit
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
        RailItem(
            selected = pantallaActual is Screen.Clientes,
            onClick = { onScreenSelected(Screen.Clientes) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Clientes") },
            label = "Clientes"
        )
        RailItem(
            selected = pantallaActual is Screen.Pedidos,
            onClick = { onScreenSelected(Screen.Pedidos) },
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Pedidos") },
            label = "Pedidos"
        )

        Spacer(modifier = Modifier.weight(1f))
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
    modifier: Modifier = Modifier
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
                Screen.Productos -> ProductoScreen()
                Screen.Clientes -> ClienteScreen()
                Screen.Pedidos -> Text(
                    text = "Pantalla de pedidos en construcción",
                    modifier = Modifier.padding(16.dp)
                )
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
    }
}
