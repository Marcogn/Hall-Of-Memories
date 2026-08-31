package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.math.absoluteValue

/**
 * Renders a hack's artwork, in order: logo over box art, else box art alone, else logo alone,
 * else a deterministic generated placeholder (spec §3.1) — never an empty box.
 */
@Composable
fun HackArtwork(
    name: String,
    boxArtPath: String?,
    logoPath: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            boxArtPath != null -> {
                AsyncImage(
                    model = boxArtPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                if (logoPath != null) {
                    AsyncImage(
                        model = logoPath,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.7f),
                    )
                }
            }
            logoPath != null -> AsyncImage(
                model = logoPath,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
            else -> HackPlaceholder(name = name, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun HackPlaceholder(name: String, modifier: Modifier = Modifier) {
    val color = remember(name) { placeholderColor(name) }
    Box(modifier = modifier.background(color), contentAlignment = Alignment.Center) {
        Text(
            text = initialsOf(name),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
    }
}

/** Deterministic so the same hack always renders the same placeholder colour. */
private fun placeholderColor(name: String): Color {
    val hue = (name.hashCode().absoluteValue % 360).toFloat()
    return Color.hsv(hue = hue, saturation = 0.45f, value = 0.55f)
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
