package com.marcogn.hallofmemories.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.ComingSoonScreen
import com.marcogn.hallofmemories.ui.home.HomeScreen
import com.marcogn.hallofmemories.ui.settings.SettingsScreen
import com.marcogn.hallofmemories.ui.templates.TemplatesScreen
import kotlinx.coroutines.launch

/**
 * A [ModalNavigationDrawer] wraps the whole [NavHost]; `drawerState` is hoisted here so every
 * drawer-reachable screen gets only an `onMenuClick` lambda, never the drawer state itself (UDF).
 * Drawer navigation uses `popUpTo(startDestination) { saveState = true }` +
 * `launchSingleTop = true` + `restoreState = true` so switching sections never grows the back
 * stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallOfMemoriesNavGraph(navController: NavHostController = rememberNavController()) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateFromDrawer(destination: Destination) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_home)) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Home) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CatchingPokemon, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_templates)) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Templates) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_settings)) },
                    selected = false,
                    onClick = { navigateFromDrawer(Destination.Settings) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        },
    ) {
        val onMenuClick: () -> Unit = { scope.launch { drawerState.open() } }

        NavHost(navController = navController, startDestination = Destination.Home) {
            composable<Destination.Home> {
                HomeScreen(onMenuClick = onMenuClick)
            }
            composable<Destination.Templates> {
                TemplatesScreen(onMenuClick = onMenuClick)
            }
            composable<Destination.Settings> {
                SettingsScreen(onMenuClick = onMenuClick)
            }
            composable<Destination.HackDetail> {
                ComingSoonScreen(phase = 2)
            }
            composable<Destination.HackForm> {
                ComingSoonScreen(phase = 2)
            }
            composable<Destination.HofDetail> {
                ComingSoonScreen(phase = 3)
            }
            composable<Destination.HofForm> {
                ComingSoonScreen(phase = 3)
            }
        }
    }
}
