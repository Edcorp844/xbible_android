package com.example.xbible.feature.store.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xbible.viewmodel.InstalledCollectionNode
import uniffi.xbible_engine.AudioCollection
import uniffi.xbible_engine.StoreItem

@Composable
fun CollectionTile(
    title: String,
    subtitle: String,
    thumbnail: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            thumbnail()
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
    }
}

// From a raw AudioCollection (store list).
@Composable
fun CollectionTile(
    collection: AudioCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = CollectionTile(
    title = collection.name,
    subtitle = collection.description ?: "Audio Collection",
    thumbnail = { CollectionThumbnail(collection, Modifier.fillMaxSize()) },
    onClick = onClick,
    modifier = modifier,
)

// From a derived InstalledCollectionNode (installed list).
@Composable
fun CollectionTile(
    node: InstalledCollectionNode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = CollectionTile(
    title = node.name,
    subtitle = node.description ?: "Audio Collection",
    thumbnail = { CollectionThumbnail(node, Modifier.fillMaxSize()) }, // see note
    onClick = onClick,
    modifier = modifier,
)

// From a derived StoreItem (store list).
@Composable
fun CollectionTile(
    item: StoreItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) = CollectionTile(
    title = item.name,
    subtitle = item.description ?: "Store Item",
    thumbnail = { CollectionThumbnail(item, Modifier.fillMaxSize()) }, // see note
    onClick = onClick,
    modifier = modifier,
    trailing= trailing
)




