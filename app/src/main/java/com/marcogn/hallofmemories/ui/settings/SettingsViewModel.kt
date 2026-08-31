package com.marcogn.hallofmemories.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.data.backup.BackupArchiveMissingDataException
import com.marcogn.hallofmemories.data.backup.LocalBackupManager
import com.marcogn.hallofmemories.data.settings.SpritePreferences
import com.marcogn.hallofmemories.data.thegamesdb.TheGamesDbPreferences
import com.marcogn.hallofmemories.domain.backup.BackupFormatTooNewException
import com.marcogn.hallofmemories.domain.repository.PokedexRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val spritePreferences: SpritePreferences,
    private val pokedexRepository: PokedexRepository,
    private val theGamesDbPreferences: TheGamesDbPreferences,
    private val localBackupManager: LocalBackupManager,
) : ViewModel() {

    private val isBackupBusy = MutableStateFlow(false)
    private val backupMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        spritePreferences.alwaysUseLatestSprites,
        pokedexRepository.observeStageStatuses(),
        pokedexRepository.syncState,
        isBackupBusy,
        backupMessage,
    ) { alwaysUseLatest, stageStatuses, syncState, backupBusy, message ->
        SettingsUiState(
            alwaysUseLatestSprites = alwaysUseLatest,
            stageStatuses = stageStatuses,
            syncState = syncState,
            isBackupBusy = backupBusy,
            backupMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setAlwaysUseLatestSprites(value: Boolean) {
        viewModelScope.launch { spritePreferences.setAlwaysUseLatestSprites(value) }
    }

    fun startSyncIfNeeded() = pokedexRepository.startSyncIfNeeded()

    fun forceResync() = pokedexRepository.forceResync()

    /** Plain SharedPreferences, read once at composition — no Flow, same pattern as [com.marcogn.hallofmemories.ui.settings.currentAppLanguage]. */
    fun currentTheGamesDbApiKey(): String = theGamesDbPreferences.apiKey.orEmpty()

    fun setTheGamesDbApiKey(value: String) {
        theGamesDbPreferences.apiKey = value
    }

    fun exportBackup(destination: Uri) {
        viewModelScope.launch {
            isBackupBusy.value = true
            try {
                localBackupManager.exportTo(destination)
                backupMessage.value = appContext.getString(R.string.settings_backup_export_success)
            } catch (e: Exception) {
                backupMessage.value = appContext.getString(R.string.settings_backup_export_failed, e.message ?: e::class.simpleName.orEmpty())
            } finally {
                isBackupBusy.value = false
            }
        }
    }

    fun importBackup(source: Uri) {
        viewModelScope.launch {
            isBackupBusy.value = true
            try {
                val result = localBackupManager.importFrom(source)
                backupMessage.value = if (result.imagesSkipped > 0) {
                    appContext.getString(R.string.settings_backup_import_success_with_skipped, result.hacksImported, result.imagesSkipped)
                } else {
                    appContext.getString(R.string.settings_backup_import_success, result.hacksImported)
                }
            } catch (e: BackupFormatTooNewException) {
                backupMessage.value = appContext.getString(R.string.settings_backup_import_too_new)
            } catch (e: BackupArchiveMissingDataException) {
                backupMessage.value = appContext.getString(R.string.settings_backup_import_invalid)
            } catch (e: Exception) {
                backupMessage.value = appContext.getString(R.string.settings_backup_import_failed, e.message ?: e::class.simpleName.orEmpty())
            } finally {
                isBackupBusy.value = false
            }
        }
    }

    fun consumeBackupMessage() {
        backupMessage.value = null
    }
}
