package com.marcogn.hallofmemories.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.SyncState
import com.marcogn.hallofmemories.ui.common.EmptyState
import com.marcogn.hallofmemories.ui.common.HallOfMemoriesTopBar
import com.marcogn.hallofmemories.ui.common.displayName

/**
 * Hack library. No hacks exist yet in this phase (hack CRUD lands in Phase 2), so this always
 * renders the empty state; the FAB is wired up once hack creation exists.
 *
 * The pokédex sync banner is non-blocking (CoverDex blocks its whole UI during its much larger
 * first fetch; this app's sync is ~2.5 MB and everything else here works without it — see
 * `docs/implementation-decisions.md`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val syncState by viewModel.syncState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { HallOfMemoriesTopBar(title = stringResource(R.string.home_title), onMenuClick = onMenuClick) },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Phase 2: navigate to HackForm */ }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PokedexSyncBanner(syncState = syncState, onRetry = viewModel::retrySync)
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_empty_subtitle),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PokedexSyncBanner(syncState: SyncState, onRetry: () -> Unit) {
    when (syncState) {
        is SyncState.Running -> Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.home_pokedex_syncing,
                        syncState.stage.displayName(),
                        syncState.done,
                        syncState.total,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                LinearProgressIndicator(
                    progress = { if (syncState.total > 0) syncState.done.toFloat() / syncState.total else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
        is SyncState.Failed -> Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_pokedex_sync_failed, syncState.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.home_pokedex_retry)) }
            }
        }
        else -> {}
    }
}
