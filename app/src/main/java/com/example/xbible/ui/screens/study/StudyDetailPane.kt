package com.example.xbible.ui.screens.study

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.QuoteSpan
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.xbible.ui.components.SectionView
import com.example.xbible.viewmodel.EngineViewModel
import com.example.xbible.viewmodel.StudyTool
import uniffi.xbible_engine.DictionaryResult
import uniffi.xbible_engine.Word

@Composable
fun SplitDetailPane(
    engineViewModel: EngineViewModel,
    onDismiss: () -> Unit
) {
    val selectedTab by engineViewModel.selectedTool.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Primary Tabs Header
        StudyToolPicker(
            selectedTab = selectedTab,
            onTabSelected = { engineViewModel.selectTool(it) }
        )
        
        // Dynamic Content Pane
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                StudyTool.Dictionary -> DictionaryTabContent(engineViewModel)
                StudyTool.Lexicon -> LexiconTabContent(engineViewModel)
                StudyTool.Commentary -> CommentaryTabContent(engineViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyToolPicker(
    selectedTab: StudyTool,
    onTabSelected: (StudyTool) -> Unit
) {
    val toolList = StudyTool.entries
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonGroup(
                overflowIndicator = { menuState ->
                    IconButton(onClick = { menuState.show() }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                },
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                toolList.forEachIndexed { index, tool ->
                    val isSelected = selectedTab == tool
                    customItem(
                        buttonGroupContent = {
                            val shapes = when {
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == toolList.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                            OutlinedToggleButton(
                                checked = isSelected,
                                onCheckedChange = { if (it) onTabSelected(tool) },
                                shapes = shapes
                            ) {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        },
                        menuContent = { menuState ->
                            DropdownMenuItem(
                                text = { Text(tool.name) },
                                onClick = {
                                    onTabSelected(tool)
                                    menuState.dismiss()
                                }
                            )
                        }
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun DictionaryTabContent(engineViewModel: EngineViewModel) {
    val selectedWord by engineViewModel.selectedWordForLookup.collectAsState()
    val results by engineViewModel.dictionaryResults.collectAsState()
    val isLoading by engineViewModel.isDictionaryLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedWord.isNotEmpty()) {
                Column {
                    Text(text = selectedWord, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(24.dp).height(3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
            } else {
                Text(text = "Select a word to lookup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedWord.isEmpty()) {
            DictionaryEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No definitions found for \"$selectedWord\".", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(results) { result -> DictionaryResultRow(result) }
            }
        }
    }
}

@Composable
fun DictionaryResultRow(result: DictionaryResult) {
    val clipboard = LocalClipboardManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text(text = result.moduleName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString("${result.key}\n\n${result.definition}")) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = result.key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            HtmlText(html = result.definition, key = result.key)
        }
    }
}

@Composable
fun DictionaryEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Dictionary Lookup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Click any word in the scripture view to automatically look up its definition across all matching installed dictionary modules.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun LexiconTabContent(engineViewModel: EngineViewModel) {
    val selectedStrongs by engineViewModel.selectedStrongsForLookup.collectAsState()
    val selectedModule by engineViewModel.selectedLexiconModule.collectAsState()
    val availableLexicons by engineViewModel.availableLexicons.collectAsState()
    val results by engineViewModel.lexiconResults.collectAsState()
    val isLoading by engineViewModel.isLexiconLoading.collectAsState()
    var showModuleMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Lexicon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (availableLexicons.isEmpty()) {
                        Text(text = "No lexicons installed", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Box {
                            Surface(onClick = { showModuleMenu = true }, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = selectedModule?.description?.takeIf { it.isNotEmpty() } ?: selectedModule?.name ?: "Select Lexicon", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(expanded = showModuleMenu, onDismissRequest = { showModuleMenu = false }) {
                                availableLexicons.forEach { module ->
                                    DropdownMenuItem(text = { Text(module.description.ifEmpty { module.name }) }, onClick = { engineViewModel.selectLexiconModule(module); showModuleMenu = false }, trailingIcon = if (selectedModule?.name == module.name) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null)
                                }
                            }
                        }
                    }
                }
                if (selectedStrongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Strong's Code: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = selectedStrongs, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedStrongs.isEmpty()) {
            LexiconEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "No lexicon entries found for \"$selectedStrongs\" in ${selectedModule?.name}.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(results) { result ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = result.moduleName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "(${result.key})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HtmlText(html = result.definition, key = result.key, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun LexiconEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Lexicon Study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Click on a Strong's number below any Greek/Hebrew word to see its lexicon entry, morphological information, and detailed translation notes.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun CommentaryTabContent(engineViewModel: EngineViewModel) {
    val selectedModule by engineViewModel.selectedCommentaryModule.collectAsState()
    val availableCommentaries by engineViewModel.availableCommentaries.collectAsState()
    val results by engineViewModel.commentaryResults.collectAsState()
    val isLoading by engineViewModel.isCommentaryLoading.collectAsState()
    val currentReference by engineViewModel.currentCommentaryReference.collectAsState()
    var showModuleMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Commentary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (availableCommentaries.isEmpty()) {
                        Text(text = "No commentaries installed", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Box {
                            Surface(onClick = { showModuleMenu = true }, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = selectedModule?.description?.takeIf { it.isNotEmpty() } ?: selectedModule?.name ?: "Select Commentary", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(expanded = showModuleMenu, onDismissRequest = { showModuleMenu = false }) {
                                availableCommentaries.forEach { module ->
                                    DropdownMenuItem(text = { Text(module.description.ifEmpty { module.name }) }, onClick = { engineViewModel.selectCommentaryModule(module); showModuleMenu = false }, trailingIcon = if (selectedModule?.name == module.name) { { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) } } else null)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Reference: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = currentReference, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (results.isNotEmpty()) {
                        Button(onClick = { 
                            val text = results.joinToString("\n\n") { section ->
                                val title = section.title.joinToString(" ") { it.text }
                                val content = section.verses.joinToString("\n") { verse -> "[${verse.number}] ${verse.words.joinToString(" ") { it.text }}" }
                                "$title\n$content"
                            }
                            clipboard.setText(AnnotatedString(text))
                        }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(24.dp), colors = ButtonDefaults.textButtonColors()) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (selectedModule == null) {
            CommentaryEmptyState()
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(text = "No commentary found for \"$currentReference\" in ${selectedModule?.name}.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(results) { section -> SectionView(section = section) }
            }
        }
    }
}

@Composable
fun CommentaryEmptyState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Commentaries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Select or install a commentary module from the Store to read study notes, theological essays, and explanations for the active scripture chapter.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun HtmlText(
    html: String,
    key: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    val spanned = remember(html, key, onSurface, primary, secondary, outlineVariant, surfaceVariant) {
        var text = html.trim()
        val keyLower = key.lowercase()
        // Clean prefix/suffix search key
        if (text.lowercase().startsWith(keyLower)) text = text.drop(keyLower.length).trim()
        if (text.lowercase().endsWith(keyLower)) text = text.dropLast(keyLower.length).trim()
        
        // Prepare HTML for parsing
        val processedHtml = text
            .replace("class=\"orth\"", "color='#${String.format("%06X", 0xFFFFFF and primary.toArgb())}'")
            .replace("class=\"pos\"", "color='#${String.format("%06X", 0xFFFFFF and secondary.toArgb())}'")
            .replace("<div class=\"cit\">", "<blockquote>")
            .replace("</div>", "</blockquote>")
            .replace("class=\"cit\"", "")

        val rawSpanned = HtmlCompat.fromHtml(processedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val spannable = SpannableStringBuilder(rawSpanned)
        
        // Post-process QuoteSpans for a professional "Full Width" block look with padding
        val quoteSpans = spannable.getSpans(0, spannable.length, QuoteSpan::class.java)
        for (span in quoteSpans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            
            spannable.removeSpan(span)
            
            // Inject our custom Block Quote Span for background + stripe + padding
            spannable.setSpan(
                BlockQuoteSpan(
                    backgroundColor = surfaceVariant.copy(alpha = 0.25f).toArgb(),
                    stripeColor = outlineVariant.toArgb(),
                    stripeWidth = 8,
                    gapWidth = 24
                ),
                start, end, flags
            )
        }
        
        spannable
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
                setTextColor(onSurface.toArgb())
                setLineSpacing(0f, 1.2f)
                // Internal padding for the whole TextView
                setPadding(0, 0, 0, 0) 
                if (style.fontFamily == FontFamily.Serif) {
                    setTypeface(android.graphics.Typeface.SERIF)
                }
            }
        },
        update = { it.text = spanned }
    )
}

/**
 * Custom Span to provide a professional full-width block quote appearance.
 */
private class BlockQuoteSpan(
    private val backgroundColor: Int,
    private val stripeColor: Int,
    private val stripeWidth: Int,
    private val gapWidth: Int
) : LeadingMarginSpan, LineBackgroundSpan {

    override fun getLeadingMargin(first: Boolean): Int = stripeWidth + gapWidth

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout
    ) {
        val originalStyle = p.style
        val originalColor = p.color

        p.style = Paint.Style.FILL
        p.color = stripeColor
        
        // Draw the vertical stripe
        c.drawRect(x.toFloat(), top.toFloat(), (x + dir * stripeWidth).toFloat(), bottom.toFloat(), p)

        p.style = originalStyle
        p.color = originalColor
    }

    override fun drawBackground(
        c: Canvas, p: Paint,
        left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        val originalColor = p.color
        p.color = backgroundColor
        
        // Draw full-width background
        c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), p)
        
        p.color = originalColor
    }
}
