package com.marcogn.hallofmemories.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.hack.HackDetailScreen
import com.marcogn.hallofmemories.ui.hack.HackFormScreen
import com.marcogn.hallofmemories.ui.hof.HofDetailScreen
import com.marcogn.hallofmemories.ui.hof.HofFormScreen
import com.marcogn.hallofmemories.ui.home.HomeScreen
import com.marcogn.hallofmemories.ui.settings.SettingsScreen
import com.marcogn.hallofmemories.ui.templates.TemplatesScreen
import kotlinx.coroutines.launch

// A NavBackStackEntry only reaches RESUMED once its enter/exit transition animation has fully
// completed and it is settled on top of the back stack. Gating every navigate()/popBackStack()
// call behind this check on the *specific* entry that owns the callback is the officially
// recommended fix for a fast double-tap landing on a screen that is still being composed/torn
// down mid-transition instead of the intended one — ported from ThePatientGamerHelper, which hit
// the same race.
private fun NavBackStackEntry.lifecycleIsResumed() =
    lifecycle.currentState == Lifecycle.State.RESUMED

private const val NAV_ANIM_DURATION_MS = 300

private val navEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> fullWidth },
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION_MS))
}
private val navPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        animationSpec = tween(NAV_ANIM_DURATION_MS, easing = FastOutSlowInEasing),
        targetOffsetX = { fullWidth -> fullWidth },
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION_MS))
}

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
        // Guards against a drawer tap landing while the current screen is still mid transition —
        // same race as any other forward/back tap, see lifecycleIsResumed() above.
        if (navController.currentBackStackEntry?.lifecycleIsResumed() != false) {
            navController.navigate(destination) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            scope.launch { drawerState.close() }
        }
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

        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            enterTransition = navEnterTransition,
            exitTransition = navExitTransition,
            popEnterTransition = navPopEnterTransition,
            popExitTransition = navPopExitTransition,
        ) {
            composable<Destination.Home> { entry ->
                HomeScreen(
                    onMenuClick = onMenuClick,
                    onAddHack = { if (entry.lifecycleIsResumed()) navController.navigate(Destination.HackForm(hackId = null)) },
                    onHackClick = { hackId -> if (entry.lifecycleIsResumed()) navController.navigate(Destination.HackDetail(hackId)) },
                )
            }
            composable<Destination.Templates> {
                TemplatesScreen(onMenuClick = onMenuClick)
            }
            composable<Destination.Settings> {
                SettingsScreen(onMenuClick = onMenuClick)
            }
            composable<Destination.HackDetail> { entry ->
                HackDetailScreen(
                    onBack = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                    onEdit = { hackId -> if (entry.lifecycleIsResumed()) navController.navigate(Destination.HackForm(hackId = hackId)) },
                    onDeleted = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                    onAddEntry = { hackId ->
                        if (entry.lifecycleIsResumed()) navController.navigate(Destination.HofForm(hackId = hackId, entryId = null))
                    },
                    onEntryClick = { entryId -> if (entry.lifecycleIsResumed()) navController.navigate(Destination.HofDetail(entryId)) },
                )
            }
            composable<Destination.HackForm> { backStackEntry ->
                val isCreating = backStackEntry.toRoute<Destination.HackForm>().hackId == null
                HackFormScreen(
                    onSaved = { id ->
                        if (backStackEntry.lifecycleIsResumed()) {
                            navController.popBackStack()
                            // Editing returns to the HackDetail already underneath in the back stack
                            // (it re-observes the same id and refreshes on its own); creating has
                            // nothing underneath yet, so it's pushed here.
                            if (isCreating) navController.navigate(Destination.HackDetail(id))
                        }
                    },
                    onCancel = { if (backStackEntry.lifecycleIsResumed()) navController.popBackStack() },
                )
            }
            composable<Destination.HofDetail> { entry ->
                HofDetailScreen(
                    onBack = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                    onEdit = { hackId, entryId ->
                        if (entry.lifecycleIsResumed()) navController.navigate(Destination.HofForm(hackId = hackId, entryId = entryId))
                    },
                    onDeleted = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                )
            }
            composable<Destination.HofForm> { entry ->
                HofFormScreen(
                    onSaved = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                    onCancel = { if (entry.lifecycleIsResumed()) navController.popBackStack() },
                )
            }
        }
    }
}
