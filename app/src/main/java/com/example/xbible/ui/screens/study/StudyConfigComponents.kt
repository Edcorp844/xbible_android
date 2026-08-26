package com.example.xbible.ui.screens.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xbible.ui.components.AddedWordStyle
import com.example.xbible.ui.components.WordConfig
import uniffi.xbible_engine.SwordModule

@Composable
fun FontSettingsContent(
    config: WordConfig,
    onConfigChange: (WordConfig) -> Unit,
    onResetConfig: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Text Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        SettingsGroup(title = "FONT") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Size", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    Slider(
                        value = config.fontSize,
                        onValueChange = { onConfigChange(config.copy(fontSize = it)) },
                        valueRange = 12f..32f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Family
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("Family", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    val families = listOf(
                        "Default" to FontFamily.Default,
                        "Serif" to FontFamily.Serif,
                        "Sans" to FontFamily.SansSerif,
                        "Mono" to FontFamily.Monospace,
                        "Cursive" to FontFamily.Cursive
                    )
                    FlowRow(modifier = Modifier.weight(1f)) {
                        families.forEach { (name, family) ->
                            FilterChip(
                                selected = config.fontFamily == family,
                                onClick = { onConfigChange(config.copy(fontFamily = family)) },
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                }

                // Line Height
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Line", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    Slider(
                        value = config.lineHeight,
                        onValueChange = { onConfigChange(config.copy(lineHeight = it)) },
                        valueRange = 1f..2.5f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Word Spacing
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Space", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    Slider(
                        value = config.wordSpacing,
                        onValueChange = { onConfigChange(config.copy(wordSpacing = it)) },
                        valueRange = 0f..10f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(title = "DISPLAY OPTIONS") {
            Column {
                SwitchItem(
                    label = "Show Lemma",
                    checked = config.showLemma,
                    onCheckedChange = { onConfigChange(config.copy(showLemma = it)) }
                )
                SwitchItem(
                    label = "Show Morph",
                    checked = config.showMorph,
                    onCheckedChange = { onConfigChange(config.copy(showMorph = it)) }
                )
                SwitchItem(
                    label = "Show Verse Notes",
                    checked = config.showVerseNotes,
                    onCheckedChange = { onConfigChange(config.copy(showVerseNotes = it)) }
                )
                SwitchItem(
                    label = "Show Word Notes",
                    checked = config.showWordNotes,
                    onCheckedChange = { onConfigChange(config.copy(showWordNotes = it)) }
                )
                SwitchItem(
                    label = "Christ's words in red",
                    checked = config.showRedWords,
                    onCheckedChange = { onConfigChange(config.copy(showRedWords = it)) }
                )
                
                // Added Words Style
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Added Words", style = MaterialTheme.typography.labelMedium) },
                    trailingContent = {
                        Row {
                            AddedWordStyle.values().forEach { style ->
                                FilterChip(
                                    selected = config.addedWordsStyle == style,
                                    onClick = { onConfigChange(config.copy(addedWordsStyle = style)) },
                                    label = { Text(style.name, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(title = "TEXT STYLE") {
            Column {
                SwitchItem(
                    label = "Justify Text",
                    checked = config.justify,
                    onCheckedChange = { onConfigChange(config.copy(justify = it)) }
                )
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
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Reset to Defaults")
        }
    }
}

@Composable
fun SwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun ParallelModeContent(
    installedBibles: List<SwordModule>,
    currentModule: SwordModule?,
    parallelModule: SwordModule?,
    onParallelModuleSelected: (SwordModule?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Parallel Display",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        SettingsGroup(title = "SELECT PARALLEL BIBLE") {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    ListItem(
                        modifier = Modifier.clickable { onParallelModuleSelected(null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("None") },
                        trailingContent = {
                            RadioButton(selected = parallelModule == null, onClick = null)
                        }
                    )
                }
                items(installedBibles) { module ->
                    if (module.name != currentModule?.name) {
                        val isSelected = parallelModule?.name == module.name
                        ListItem(
                            modifier = Modifier.clickable { onParallelModuleSelected(module) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(module.name) },
                            supportingContent = { Text(module.description) },
                            trailingContent = {
                                RadioButton(selected = isSelected, onClick = null)
                            }
                        )
                    }
                }
            }
        }
    }
}
