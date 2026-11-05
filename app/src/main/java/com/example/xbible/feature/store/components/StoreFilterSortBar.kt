package com.example.xbible.feature.store.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StoreFilter(val label: String) {
    ALL("All"),
    INSTALLED("Installed"),
    NOT_INSTALLED("Not installed"),
}

enum class StoreSort(val label: String) {
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A"),
    BOOKS_DESC("Most books"),
    VERSES_DESC("Most verses"),
    LANGUAGE("Language"),
}

/** Search-only header. Filter + sort live in the top bar. */
@Composable
fun StoreSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        } else null,
        shape = CircleShape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
        singleLine = true,
    )
}

/** Applies filter + sort to a list of [uniffi.xbible_engine.StoreItem]. */
fun List<uniffi.xbible_engine.StoreItem>.applyFilterAndSort(
    filter: StoreFilter,
    sort: StoreSort,
): List<uniffi.xbible_engine.StoreItem> {
    val filtered = when (filter) {
        StoreFilter.ALL -> this
        StoreFilter.INSTALLED -> filter { it.installed }
        StoreFilter.NOT_INSTALLED -> filter { !it.installed }
    }
    return when (sort) {
        StoreSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        StoreSort.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        StoreSort.BOOKS_DESC -> filtered.sortedByDescending { it.bookCount }
        StoreSort.VERSES_DESC -> filtered.sortedByDescending { it.verseCount }
        StoreSort.LANGUAGE -> filtered.sortedWith(
            compareBy({ it.language.lowercase() }, { it.name.lowercase() })
        )
    }
}