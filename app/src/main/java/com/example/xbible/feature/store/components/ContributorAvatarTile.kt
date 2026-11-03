package com.example.xbible.feature.store.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uniffi.xbible_engine.AudioContributor
import uniffi.xbible_engine.ContributorRole

@Composable
fun ContributorAvatarTile(
    contributor: AudioContributor,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape),
        ) {
            val url = contributor.photo?.url
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = contributor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val initials = contributor.name.split(" ").take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        initials,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                contributor.role.displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun ContributorRole.displayName(): String = when (this) {
    ContributorRole.NARRATOR -> "Narrator"
    ContributorRole.AUTHOR -> "Author"
    ContributorRole.PRODUCER -> "Producer"
    ContributorRole.TRANSLATOR -> "Translator"
    ContributorRole.EDITOR -> "Editor"
}