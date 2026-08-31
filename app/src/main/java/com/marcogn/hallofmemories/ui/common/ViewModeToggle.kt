package com.marcogn.hallofmemories.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R
import com.marcogn.hallofmemories.domain.model.ViewMode

/** Shows the icon for the mode a tap would switch *to*. */
@Composable
fun ViewModeToggle(viewMode: ViewMode, onViewModeChange: (ViewMode) -> Unit) {
    when (viewMode) {
        ViewMode.LIST -> IconButton(onClick = { onViewModeChange(ViewMode.GRID) }) {
            Icon(Icons.Filled.GridView, contentDescription = stringResource(R.string.cd_view_mode_grid))
        }
        ViewMode.GRID -> IconButton(onClick = { onViewModeChange(ViewMode.LIST) }) {
            Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = stringResource(R.string.cd_view_mode_list))
        }
    }
}
