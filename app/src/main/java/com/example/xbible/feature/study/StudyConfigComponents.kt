package com.example.xbible.feature.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.R
import com.example.xbible.core.ui.components.AddedWordStyle
import com.example.xbible.core.ui.components.VerseLayout
import com.example.xbible.core.ui.components.WordConfig
import uniffi.xbible_engine.InstalledTranslation

// ── Font / text settings ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingsContent(
    config: WordConfig,
    onConfigChange: (WordConfig) -> Unit,
    onResetConfig: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.text_settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )


        // ── Font Settings ─────────────────────────────────────────
        SettingsGroup(title = stringResource(R.string.font)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SliderRow(
                    label = stringResource(R.string.size),
                    currentValue = config.fontSize,
                    valueRange = 12f..32f,
                    valueSuffix = "sp",
                    onValueChange = { onConfigChange(config.copy(fontSize = it)) },
                )

                // Family
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.family),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(70.dp),
                    )
                    val families = listOf(
                        "Default" to FontFamily.Default,
                        "Serif" to FontFamily.Serif,
                        "Sans" to FontFamily.SansSerif,
                        "Mono" to FontFamily.Monospace,
                        "Cursive" to FontFamily.Cursive,
                    )
                    FlowRow(modifier = Modifier.weight(1f)) {
                        families.forEach { (name, family) ->
                            FilterChip(
                                selected = config.fontFamily == family,
                                onClick = { onConfigChange(config.copy(fontFamily = family)) },
                                label = {
                                    Text(name, style = MaterialTheme.typography.labelSmall)
                                },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Reading Layout ────────────────────────────────────────
        SettingsGroup(title = stringResource(R.string.verse_layout)) {
            Column() {
                SwitchItem(
                    label = stringResource(R.string.verse_per_line),
                    checked = config.verseLayout == VerseLayout.VersePerLine,
                    onCheckedChange = {
                        onConfigChange(
                            config.copy(
                                verseLayout = if (it) {
                                    VerseLayout.VersePerLine
                                } else {
                                    VerseLayout.Paragraph
                                }
                            )
                        )
                    },
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SliderRow(
                    label = stringResource(R.string.line_height),
                    currentValue = config.lineHeight,
                    steps = 10,
                    valueRange = 1f..2.5f,
                    valueSuffix = "x",
                    onValueChange = { onConfigChange(config.copy(lineHeight = it)) },
                )

//                SliderRow(
//                    label = stringResource(R.string.word_spacing),
//                    currentValue = config.wordSpacing,
//                    valueRange = 0f..10f,
//                    valueSuffix = "dp",
//                    onValueChange = { onConfigChange(config.copy(wordSpacing = it)) },
//                )

                SliderRow(
                    label = stringResource(R.string.verse_spacing),
                    currentValue = config.verseSpacing,
                    steps = 10,
                    valueRange = 0f..1.0f,
                    valueSuffix = "dp",
                    config.verseLayout == VerseLayout.VersePerLine,
                    onValueChange = { onConfigChange(config.copy(verseSpacing = it)) },
                )

            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsGroup(title = ""){
            SwitchItem(
                label = stringResource(R.string.justify_text),
                checked = config.justify,
                onCheckedChange = { onConfigChange(config.copy(justify = it)) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Display Options ───────────────────────────────────────
        SettingsGroup(title = stringResource(R.string.display_options)) {
            Column {
                SwitchItem(
                    label = stringResource(R.string.show_strongs),
                    checked = config.showStrongs,
                    onCheckedChange = { onConfigChange(config.copy(showStrongs = it)) },
                )
                SwitchItem(
                    label = stringResource(R.string.show_lemma),
                    checked = config.showLemma,
                    onCheckedChange = { onConfigChange(config.copy(showLemma = it)) },
                )
                SwitchItem(
                    label = stringResource(R.string.show_morph),
                    checked = config.showMorph,
                    onCheckedChange = { onConfigChange(config.copy(showMorph = it)) },
                )
                SwitchItem(
                    label = stringResource(R.string.show_verse_notes),
                    checked = config.showVerseNotes,
                    onCheckedChange = { onConfigChange(config.copy(showVerseNotes = it)) },
                )
                SwitchItem(
                    label = stringResource(R.string.show_word_notes),
                    checked = config.showWordNotes,
                    onCheckedChange = { onConfigChange(config.copy(showWordNotes = it)) },
                )
                SwitchItem(
                    label = stringResource(R.string.red_words),
                    checked = config.showRedWords,
                    onCheckedChange = { onConfigChange(config.copy(showRedWords = it)) },
                )

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    trailingContent = {
                        Row {
                            AddedWordStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = config.addedWordsStyle == style,
                                    onClick = {
                                        onConfigChange(config.copy(addedWordsStyle = style))
                                    },
                                    label = {
                                        Text(
                                            style.name,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    },
                ) {
                    Text(
                        stringResource(R.string.added_words),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onResetConfig,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text(stringResource(R.string.reset_to_defaults))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    label: String,
    currentValue: Float,
    steps: Int = 0,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSuffix: String,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
) {
    val sliderState = rememberSliderState(
        value = currentValue,
        steps = steps,
        trackRange = valueRange,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (enabled) {Color.Unspecified} else {
                    SliderDefaults.colors().disabledThumbColor}
            ),
            modifier = Modifier.width(70.dp),
        )
        Slider(
            state = sliderState,
            onValueChange = { newValue ->
                sliderState.value = newValue
                onValueChange(newValue)
            },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (currentValue % 1f == 0f) {
                "${currentValue.toInt()}$valueSuffix"
            } else {
                String.format("%.1f", currentValue) + valueSuffix
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .width(50.dp)
                .padding(start = 8.dp),
        )
    }
}

@Composable
fun SwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Parallel translation picker ──────────────────────────────────

@Composable
fun ParallelModeContent(
    translations: List<InstalledTranslation>,
    current: InstalledTranslation?,
    parallel: InstalledTranslation?,
    onParallelSelected: (InstalledTranslation?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.parallel_display),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold,
        )

        SettingsGroup(title = stringResource(R.string.select_parallel_bible)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                item(key = "none") {
                    ListItem(
                        modifier = Modifier.clickable { onParallelSelected(null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        trailingContent = {
                            RadioButton(selected = parallel == null, onClick = null)
                        },
                    ) {
                        Text(stringResource(R.string.none))
                    }
                }
                items(
                    items = translations.filter { it.id != current?.id },
                    key = { it.id },
                ) { t ->
                    val isSelected = parallel?.id == t.id
                    ListItem(
                        modifier = Modifier.clickable { onParallelSelected(t) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        trailingContent = {
                            RadioButton(selected = isSelected, onClick = null)
                        },
                    ) {
                        Column {
                            Text(t.name)
                            val subtitle = buildString {
                                if (t.englishName.isNotBlank() && t.englishName != t.name) {
                                    append(t.englishName)
                                    append(" · ")
                                }
                                append(t.shortName)
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}