package com.marcogn.hallofmemories.ui.hack

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.ui.common.GameArtSearchDialog
import com.marcogn.hallofmemories.ui.common.GenerationPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HackFormScreen(
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: HackFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchDialog by remember { mutableStateOf(false) }

    val boxArtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onBoxArtPicked) }
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onLogoPicked) }

    fun openSearchDialog() {
        viewModel.onSearchOnlineOpened()
        showSearchDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (uiState.isEditMode) R.string.hack_form_edit_title else R.string.hack_form_new_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(onSaved) }, enabled = !uiState.isSaving) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.draft.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.hack_name_label)) },
                singleLine = true,
                isError = uiState.errorMessage != null && uiState.draft.name.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            GenerationPicker(
                selected = uiState.draft.generation,
                onSelectedChange = viewModel::onGenerationChange,
                modifier = Modifier.fillMaxWidth(),
            )

            // One search fetches both box art and logo together (GameArtSearchCoordinator already
            // downloads both from a single selected result) — a single entry point here instead of
            // a separate "search online" per artwork slot avoids implying two independent searches.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.hack_artwork_section_title), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = ::openSearchDialog) { Text(stringResource(R.string.hack_artwork_search_online)) }
            }

            if (uiState.isDownloadingArt) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.hack_artwork_downloading), style = MaterialTheme.typography.bodySmall)
                }
            }

            ArtworkSlot(
                label = stringResource(R.string.hack_artwork_box_art_label),
                imagePath = uiState.draft.boxArtPath,
                onPickFromGallery = {
                    boxArtLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = viewModel::onBoxArtRemoved,
            )

            ArtworkSlot(
                label = stringResource(R.string.hack_artwork_logo_label),
                imagePath = uiState.draft.logoPath,
                onPickFromGallery = {
                    logoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = viewModel::onLogoRemoved,
            )

            OutlinedTextField(
                value = uiState.draft.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.hack_notes_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    if (showSearchDialog) {
        GameArtSearchDialog(
            query = uiState.search.query,
            onQueryChange = viewModel::onSearchQueryChange,
            isSearching = uiState.search.isSearching,
            results = uiState.search.results,
            infoMessage = uiState.search.message,
            onSearch = viewModel::onSearchOnline,
            onResultSelected = { result ->
                viewModel.onSearchResultSelected(result)
                showSearchDialog = false
            },
            onDismiss = {
                viewModel.onSearchDialogDismissed()
                showSearchDialog = false
            },
        )
    }
}

@Composable
private fun ArtworkSlot(
    label: String,
    imagePath: String?,
    onPickFromGallery: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onPickFromGallery),
            contentAlignment = Alignment.Center,
        ) {
            if (imagePath != null) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row {
            TextButton(onClick = onPickFromGallery) { Text(stringResource(R.string.hack_artwork_choose_gallery)) }
            TextButton(onClick = onRemove, enabled = imagePath != null) { Text(stringResource(R.string.hack_artwork_remove)) }
        }
    }
}
