package com.marcogn.hallofmemories.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.sprite.SpriteUrlResolver

/**
 * Renders a Pokémon sprite for [speciesId] under [generation]/[shiny]/[alwaysUseLatest], walking
 * [SpriteUrlResolver.candidates] on load failure — Coil has no built-in fallback chain, so this
 * composable is it. Falls back to a placeholder icon once every candidate has failed to load.
 */
@Composable
fun PokemonSprite(
    speciesId: Int,
    generation: GameGeneration,
    shiny: Boolean,
    alwaysUseLatest: Boolean,
    speciesGeneration: Int?,
    modifier: Modifier = Modifier,
) {
    val candidates = remember(speciesId, generation, shiny, alwaysUseLatest, speciesGeneration) {
        SpriteUrlResolver.candidates(speciesId, generation, shiny, alwaysUseLatest, speciesGeneration)
    }
    var index by remember(candidates) { mutableIntStateOf(0) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (index < candidates.size) {
            AsyncImage(
                model = candidates[index],
                contentDescription = null,
                onError = { index++ },
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.CatchingPokemon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}
