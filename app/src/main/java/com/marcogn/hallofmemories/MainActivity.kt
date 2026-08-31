package com.marcogn.hallofmemories

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.domain.model.ThemeMode
import com.marcogn.hallofmemories.ui.navigation.HallOfMemoriesNavGraph
import com.marcogn.hallofmemories.ui.theme.HallOfMemoriesTheme
import com.marcogn.hallofmemories.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * `AppCompatActivity`, not `ComponentActivity`: `AppCompatDelegate.setApplicationLocales()`
 * (the in-app language picker, Settings) silently does nothing with the latter. Doesn't
 * introduce View/XML: `setContent {}` remains the only UI entry point. See CLAUDE.md,
 * "Known gotchas".
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HallOfMemoriesApp()
        }
    }
}

@Composable
private fun HallOfMemoriesApp(themeViewModel: ThemeViewModel = hiltViewModel()) {
    val themeMode by themeViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    HallOfMemoriesTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            HallOfMemoriesNavGraph()
        }
    }
}
