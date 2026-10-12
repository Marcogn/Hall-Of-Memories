package com.marcogn.hallofmemories.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.BuildConfig
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar

private val GITHUB_URL = "https://github.com/${BuildConfig.UPDATE_GITHUB_OWNER}/${BuildConfig.UPDATE_GITHUB_REPO}"

/**
 * Info screen: reached from the drawer like Settings, so it shares the drawer top bar and needs no
 * `BackHandler` of its own — `navigateFromDrawer` leaves Home below it, and the default back pops to it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onMenuClick: () -> Unit, viewModel: AboutViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { HallOfMemoriesTopBar(title = stringResource(R.string.about_title), onMenuClick = onMenuClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::onCheckNow, enabled = !uiState.isChecking, modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp).size(18.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                    }
                    Text(stringResource(R.string.about_check_updates))
                }
                uiState.checkMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }

            HorizontalDivider()

            Text(stringResource(R.string.about_made_with_love), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { runCatching { uriHandler.openUri(GITHUB_URL) } }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_github))
            }
            Text(stringResource(R.string.about_license), style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider()

            Text(stringResource(R.string.about_disclaimers_title), style = MaterialTheme.typography.titleMedium)
            listOf(
                R.string.about_disclaimer_unofficial,
                R.string.about_disclaimer_trademarks,
                R.string.about_disclaimer_pokeapi,
                R.string.about_disclaimer_thegamesdb,
                R.string.about_disclaimer_hacks,
            ).forEach { res ->
                Text("• ${stringResource(res)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
