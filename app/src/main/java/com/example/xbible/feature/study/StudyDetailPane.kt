package com.example.xbible.feature.study

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.QuoteSpan
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.example.xbible.R
import com.example.xbible.viewmodel.ReaderViewModel
import com.example.xbible.viewmodel.StudyTool

@Composable
fun SplitDetailPane(
    readerViewModel: ReaderViewModel,
) {
    val selectedTab by readerViewModel.selectedTool.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                StudyToolPicker(
                    selectedTab = selectedTab,
                    onTabSelected = { readerViewModel.selectTool(it) },
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        StudyTool.Dictionary -> DictionaryTabContent(readerViewModel)
                        StudyTool.Lexicon -> LexiconTabContent(readerViewModel)
                        StudyTool.Commentary -> CommentaryTabContent(readerViewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyToolPicker(
    selectedTab: StudyTool,
    onTabSelected: (StudyTool) -> Unit,
) {
    val toolList = StudyTool.entries
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ButtonGroup(
                    overflowIndicator = { menuState ->
                        IconButton(onClick = { menuState.show() }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                    },
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
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
                                CompositionLocalProvider(
                                    LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                                ) {
                                    OutlinedToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { if (it) onTabSelected(tool) },
                                        shapes = shapes,
                                        modifier = Modifier.defaultMinSize(
                                            minWidth = 1.dp,
                                            minHeight = 1.dp,
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 4.dp,
                                        ),
                                    ) {
                                        Text(
                                            text = tool.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                            else FontWeight.Medium,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            },
                            menuContent = { menuState ->
                                DropdownMenuItem(
                                    text = { Text(tool.name) },
                                    onClick = {
                                        onTabSelected(tool)
                                        menuState.dismiss()
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

// ── Dictionary ─────────────────────────────────────────────────────
//
// NOTE: `XBibleEngine.lookupDictionary` and `DictionaryResult` were
// removed from the current FFI. This tab now renders a single
// "not available" empty state. To restore it, re-export the dictionary
// API from Rust, regenerate bindings, and re-add the `dictionaryResults`
// / `isDictionaryLoading` state flows to `ReaderViewModel` (the
// `lookupWord(...)` function is already stubbed there).

@Composable
fun DictionaryTabContent(readerViewModel: ReaderViewModel) {
    val selectedWord by readerViewModel.selectedWordForLookup.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedWord.isNotEmpty()) {
                Column {
                    Text(
                        text = selectedWord,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.select_word_lookup),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        if (selectedWord.isEmpty()) {
            DictionaryEmptyState()
        } else {
            FeatureUnavailableState(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                titleRes = R.string.dictionary_lookup,
                bodyRes = R.string.dictionary_lookup_desc,
            )
        }
    }
}

@Composable
fun DictionaryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.dictionary_lookup),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.dictionary_lookup_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ── Lexicon ────────────────────────────────────────────────────────
//
// Same story as Dictionary — `lookupStrongsNumber`, `getLexiconModules`,
// `LexiconResult`, `SwordModule` are all gone from the FFI.

@Composable
fun LexiconTabContent(readerViewModel: ReaderViewModel) {
    val selectedStrongs by readerViewModel.selectedStrongsForLookup.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.lexicon),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.no_lexicons_installed),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (selectedStrongs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.strongs_code_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = selectedStrongs,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )
        if (selectedStrongs.isEmpty()) {
            LexiconEmptyState()
        } else {
            FeatureUnavailableState(
                icon = Icons.Outlined.Translate,
                titleRes = R.string.lexicon_study,
                bodyRes = R.string.lexicon_study_desc,
            )
        }
    }
}

@Composable
fun LexiconEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Translate,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.lexicon_study),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.lexicon_study_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ── Commentary ─────────────────────────────────────────────────────
//
// `getChapterContent` / `getCommentaryModules` / `Section` are all gone
// from the FFI.

@Composable
fun CommentaryTabContent(readerViewModel: ReaderViewModel) {
    val currentReference by readerViewModel.currentCommentaryReference.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.commentary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.no_commentaries_installed),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.reference_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = currentReference,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )
        FeatureUnavailableState(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            titleRes = R.string.commentaries,
            bodyRes = R.string.commentaries_desc,
        )
    }
}

@Composable
fun CommentaryEmptyState() {
    FeatureUnavailableState(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        titleRes = R.string.commentaries,
        bodyRes = R.string.commentaries_desc,
    )
}

// ── Shared "feature not available" empty state ────────────────────

@Composable
private fun FeatureUnavailableState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleRes: Int,
    bodyRes: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(bodyRes),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ── HTML rendering (kept verbatim — used when the panels come back) ──

@Composable
fun HtmlText(
    html: String,
    key: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val spanned =
        remember(html, key, onSurface, primary, secondary, outlineVariant, surfaceVariant) {
            var text = html.trim()
            val keyLower = key.lowercase()
            if (text.lowercase().startsWith(keyLower)) text = text.drop(keyLower.length).trim()
            if (text.lowercase().endsWith(keyLower)) text = text.dropLast(keyLower.length).trim()

            val processedHtml = text
                .replace(
                    "class=\"orth\"",
                    "color='#${String.format("%06X", 0xFFFFFF and primary.toArgb())}'",
                )
                .replace(
                    "class=\"pos\"",
                    "color='#${String.format("%06X", 0xFFFFFF and secondary.toArgb())}'",
                )
                .replace("<div class=\"cit\">", "<blockquote>")
                .replace("</div>", "</blockquote>")
                .replace("class=\"cit\"", "")

            val rawSpanned = HtmlCompat.fromHtml(processedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
            val spannable = SpannableStringBuilder(rawSpanned)

            val quoteSpans = spannable.getSpans(0, spannable.length, QuoteSpan::class.java)
            if (quoteSpans.isNotEmpty()) {
                val sortedSpans = quoteSpans.sortedBy { spannable.getSpanStart(it) }

                var currentStart = spannable.getSpanStart(sortedSpans[0])
                var currentEnd = spannable.getSpanEnd(sortedSpans[0])

                for (i in sortedSpans.indices) {
                    val span = sortedSpans[i]
                    val start = spannable.getSpanStart(span)
                    val end = spannable.getSpanEnd(span)

                    spannable.removeSpan(span)

                    if (start <= currentEnd + 1) {
                        currentEnd = maxOf(currentEnd, end)
                    } else {
                        applyBlockQuote(
                            spannable,
                            currentStart,
                            currentEnd,
                            surfaceVariant.toArgb(),
                            outlineVariant.toArgb(),
                        )
                        currentStart = start
                        currentEnd = end
                    }
                }
                applyBlockQuote(
                    spannable,
                    currentStart,
                    currentEnd,
                    surfaceVariant.toArgb(),
                    outlineVariant.toArgb(),
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
                setPadding(0, 0, 0, 0)
                if (style.fontFamily == FontFamily.Serif) {
                    setTypeface(android.graphics.Typeface.SERIF)
                }
            }
        },
        update = { it.text = spanned },
    )
}

private fun applyBlockQuote(
    spannable: SpannableStringBuilder,
    start: Int,
    end: Int,
    backgroundColor: Int,
    stripeColor: Int,
) {
    if (start < end) {
        val alpha = (0.25f * 255).toInt()
        val bgWithAlpha = (backgroundColor and 0x00FFFFFF) or (alpha shl 24)
        spannable.setSpan(
            BlockQuoteSpan(
                backgroundColor = bgWithAlpha,
                stripeColor = stripeColor,
                stripeWidth = 8,
                gapWidth = 24,
            ),
            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
}

private class BlockQuoteSpan(
    private val backgroundColor: Int,
    private val stripeColor: Int,
    private val stripeWidth: Int,
    private val gapWidth: Int,
) : LeadingMarginSpan, LineBackgroundSpan {

    private val horizontalMargin = 20f
    private val radius = 8f

    override fun getLeadingMargin(first: Boolean): Int =
        (stripeWidth + gapWidth + horizontalMargin).toInt()

    private fun isFirstLine(spanned: Spanned, start: Int): Boolean {
        val spanStart = spanned.getSpanStart(this)
        return start <= spanStart
    }

    private fun isLastLine(spanned: Spanned, end: Int): Boolean {
        val spanEnd = spanned.getSpanEnd(this)
        return end >= spanEnd
    }

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int,
        top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int,
        first: Boolean, layout: Layout,
    ) {
        val spanned = text as? Spanned ?: return
        val isFirst = isFirstLine(spanned, start)
        val isLast = isLastLine(spanned, end)

        val originalStyle = p.style
        val originalColor = p.color

        p.style = Paint.Style.FILL
        p.color = stripeColor

        val adjustedX = x.toFloat() + horizontalMargin
        val rectRight = adjustedX + dir * stripeWidth
        val rect = RectF(adjustedX, top.toFloat(), rectRight, bottom.toFloat())

        val path = Path()
        val radii = floatArrayOf(
            if (isFirst) radius else 0f, if (isFirst) radius else 0f,
            0f, 0f,
            0f, 0f,
            if (isLast) radius else 0f, if (isLast) radius else 0f,
        )

        path.addRoundRect(rect, radii, Path.Direction.CW)
        c.drawPath(path, p)

        p.style = originalStyle
        p.color = originalColor
    }

    override fun drawBackground(
        c: Canvas, p: Paint,
        left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int,
    ) {
        val spanned = text as? Spanned ?: return
        val isFirst = isFirstLine(spanned, start)
        val isLast = isLastLine(spanned, end)

        val originalColor = p.color
        p.color = backgroundColor

        val rectLeft = left.toFloat() + horizontalMargin
        val rectRight = right.toFloat() - horizontalMargin
        val rect = RectF(rectLeft, top.toFloat(), rectRight, bottom.toFloat())

        val path = Path()
        val radii = floatArrayOf(
            if (isFirst) radius else 0f, if (isFirst) radius else 0f,
            if (isFirst) radius else 0f, if (isFirst) radius else 0f,
            if (isLast) radius else 0f, if (isLast) radius else 0f,
            if (isLast) radius else 0f, if (isLast) radius else 0f,
        )

        path.addRoundRect(rect, radii, Path.Direction.CW)
        c.drawPath(path, p)

        p.color = originalColor
    }
}