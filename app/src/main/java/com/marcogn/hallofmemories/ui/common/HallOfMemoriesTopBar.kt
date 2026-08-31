package com.marcogn.hallofmemories.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.hallofmemories.R

/** Top bar shared by every drawer destination: title plus the hamburger menu button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallOfMemoriesTopBar(
    title: String,
    onMenuClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.cd_menu))
            }
        },
        scrollBehavior = scrollBehavior,
    )
}
