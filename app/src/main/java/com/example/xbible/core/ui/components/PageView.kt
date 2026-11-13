package com.example.xbible.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.EntityRef
import uniffi.xbible_engine.ReaderBlock
import uniffi.xbible_engine.ReaderChapter
import uniffi.xbible_engine.ReaderVerse
import uniffi.xbible_engine.ReaderWord

/**
 * Read surface for a translation chapter.
 *
 * Gesture contract:
 * - **Tap a word** → [onWordClick] / [onStrongsClick].
 * - **Tap a verse** → [onVerseTap] (optional; e.g. brief highlight).
 * - **Long-press a verse** → [onVerseLongPress], used by StudyScreen to
 *   open the verse actions sheet without interfering with normal taps.
 *
 * Both gesture paths work identically in paragraph mode and
 * verse-per-line mode. The tap-vs-long-press distinction is resolved
 * via `detectTapGestures` on the verse text, which uses
 * [TextLayoutResult.getOffsetForPosition] to map a screen coordinate
 * to a `VERSE` string annotation.
 */
@Composable
fun PageView(
    chapter: ReaderChapter,
    modifier: Modifier = Modifier,
    parallelChapter: ReaderChapter? = null,
    config: WordConfig = WordConfig(),
    state: LazyListState = rememberLazyListState(),
    onWordClick: ((ReaderWord) -> Unit)? = null,
    onStrongsClick: ((String) -> Unit)? = null,
    onVerseTap: ((Int) -> Unit)? = null,
    onVerseLongPress: ((Int) -> Unit)? = null,
    chapterEntities: List<EntityRef> = emptyList(),
    onEntityTap: ((EntityRef) -> Unit)? = null,
    selectedVerseNumber: Int? = null,
    highlights: Map<String, VerseHighlight> = emptyMap(),
) {
    val sections = remember(chapter) { groupBlocksIntoSections(chapter.blocks) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item(key = "chapter-header") {
            ChapterHeader(chapter, config)
        }

        items(sections, key = { it.id }) { section ->
            ParagraphSectionView(
                section = section,
                config = config,
                onStrongsClick = onStrongsClick,
                onVerseTap = onVerseTap,
                onVerseLongPress = onVerseLongPress,
                selectedVerseNumber = selectedVerseNumber,
                highlights = highlights,
                bookId = chapter.bookId,
                chapterNumber = chapter.chapterNumber,
            )
        }

        if (chapterEntities.isNotEmpty()) {
            item(key = "chapter-footer") {
                ChapterFooter(
                    chapter = chapter,
                    entities = chapterEntities,
                    onEntityTap = onEntityTap,
                )
            }
        }
    }
}

// ── Data grouping ─────────────────────────────────────────────────

private data class ParagraphSection(
    val id: String,
    val headingText: String? = null,
    val subtitleText: String? = null,
    val verses: List<ReaderVerse> = emptyList(),
)

private fun groupBlocksIntoSections(blocks: List<ReaderBlock>): List<ParagraphSection> {
    val sections = mutableListOf<ParagraphSection>()
    var currentHeading: String? = null
    var currentSubtitle: String? = null
    val currentVerses = mutableListOf<ReaderVerse>()
    var sectionIndex = 0

    fun commitSection() {
        if (currentHeading != null || currentSubtitle != null || currentVerses.isNotEmpty()) {
            sections.add(
                ParagraphSection(
                    id = "section-$sectionIndex",
                    headingText = currentHeading,
                    subtitleText = currentSubtitle,
                    verses = currentVerses.toList(),
                )
            )
            sectionIndex++
            currentHeading = null
            currentSubtitle = null
            currentVerses.clear()
        }
    }

    blocks.forEach { block ->
        when (block.itemType) {
            "heading" -> {
                commitSection()
                currentHeading = block.text
            }
            "hebrew_subtitle" -> {
                commitSection()
                currentSubtitle = block.text
            }
            "verse" -> block.verses.forEach { currentVerses.add(it) }
            else -> Unit
        }
    }
    commitSection()
    return sections
}

// ── Chapter header ─────────────────────────────────────────────────

@Composable
private fun ChapterHeader(chapter: ReaderChapter, config: WordConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = chapter.bookName.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = config.fontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.outlineVariant)
                        )
                    )
            )
            Text(
                text = chapter.chapterNumber.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = config.fontFamily,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.outlineVariant, Color.Transparent)
                        )
                    )
            )
        }
    }
}

// ── Section renderer ──────────────────────────────────────────────

@Composable
private fun ParagraphSectionView(
    section: ParagraphSection,
    config: WordConfig,
    onStrongsClick: ((String) -> Unit)?,
    onVerseTap: ((Int) -> Unit)?,
    onVerseLongPress: ((Int) -> Unit)?,
    selectedVerseNumber: Int?,
    highlights: Map<String, VerseHighlight>,
    bookId: String,
    chapterNumber: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        section.headingText?.let { heading ->
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = config.fontFamily,
                    fontSize = config.fontSize.sp * 1.025f,
                    lineHeight = (config.fontSize * config.lineHeight).sp * 1.025f,
                    textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = if (section.subtitleText == null) 4.dp else 12.dp,
                ),
            )
        }

        section.subtitleText?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = config.fontFamily,
                    fontSize = config.fontSize.sp * 0.8f,
                    lineHeight = (config.fontSize * config.lineHeight).sp * 0.8f,
                    textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                ),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = if (section.headingText == null) 0.dp else 8.dp,
                    bottom = 8.dp,
                ),
            )
        }

        if (section.verses.isEmpty()) return@Column

        if (config.verseLayout == VerseLayout.VersePerLine) {
            VersePerLineLayout(
                verses = section.verses,
                config = config,
                onStrongsClick = onStrongsClick,
                onVerseTap = onVerseTap,
                onVerseLongPress = onVerseLongPress,
                selectedVerseNumber = selectedVerseNumber,
                highlights = highlights,
                bookId = bookId,
                chapterNumber = chapterNumber,
            )
        } else {
            ParagraphLayout(
                verses = section.verses,
                config = config,
                onStrongsClick = onStrongsClick,
                onVerseTap = onVerseTap,
                onVerseLongPress = onVerseLongPress,
                selectedVerseNumber = selectedVerseNumber,
                highlights = highlights,
                bookId = bookId,
                chapterNumber = chapterNumber,
            )
        }
    }
}

// ── Verse-per-line layout ─────────────────────────────────────────

@Composable
private fun VersePerLineLayout(
    verses: List<ReaderVerse>,
    config: WordConfig,
    onStrongsClick: ((String) -> Unit)?,
    onVerseTap: ((Int) -> Unit)?,
    onVerseLongPress: ((Int) -> Unit)?,
    selectedVerseNumber: Int?,
    highlights: Map<String, VerseHighlight>,
    bookId: String,
    chapterNumber: Int,
) {
    val verseSpacing = (config.fontSize * config.verseSpacing).dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verseSpacing),
    ) {
        verses.forEach { verse ->
            val isSelected = verse.number == selectedVerseNumber
            val highlight = highlights["$bookId:$chapterNumber:${verse.number}"]

            // Build the row modifier (highlight bg + underline + gesture)
            // inside the composable so theme colors are available.
            val backgroundModifier: Modifier = when {
                isSelected -> Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp),
                )
                highlight?.style == HighlightStyle.Solid -> Modifier.background(
                    highlight.color.color.copy(alpha = 0.35f),
                    RoundedCornerShape(8.dp),
                )
                highlight?.style == HighlightStyle.Marker -> Modifier.background(
                    highlight.color.color.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp),
                )
                else -> Modifier
            }

            val underlineModifier: Modifier =
                if (highlight?.style == HighlightStyle.Underline) {
                    Modifier.drawBehind {
                        drawLine(
                            color = highlight.color.color,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 3.dp.toPx(),
                        )
                    }
                } else Modifier

            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            val verseText = remember(verse, config, isSelected, highlight) {
                buildAnnotatedString {
                    pushStringAnnotation("VERSE", verse.number.toString())
                    if (config.showVerseNumbers && verse.number > 0) {
                        appendVerseNumber(
                            verse = verse,
                            config = config,
                            highlight = highlight,
                            isSelected = isSelected,
                            inline = false,
                            primaryColor = Color.Unspecified,
                            onSurfaceVariant = Color.Unspecified,
                        )
                    }
                    appendVerseBody(verse, config, highlight, isSelected)
                    pop()
                }
            }

            val textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = config.fontFamily,
                fontSize = config.fontSize.sp,
                lineHeight = (config.fontSize * config.lineHeight).sp,
                textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
            )
            val textColor = config.primaryTextColor.takeIf { it != Color.Unspecified }
                ?: MaterialTheme.colorScheme.onSurface

            Text(
                text = verseText,
                style = textStyle,
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(backgroundModifier)
                    .then(underlineModifier)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .pointerInput(verse.number) {
                        detectTapGestures(
                            onTap = { onVerseTap?.invoke(verse.number) },
                            onLongPress = { onVerseLongPress?.invoke(verse.number) },
                        )
                    },
                onTextLayout = { layout = it },
            )
        }
    }
}

// ── Paragraph layout ──────────────────────────────────────────────

@Composable
private fun ParagraphLayout(
    verses: List<ReaderVerse>,
    config: WordConfig,
    onStrongsClick: ((String) -> Unit)?,
    onVerseTap: ((Int) -> Unit)?,
    onVerseLongPress: ((Int) -> Unit)?,
    selectedVerseNumber: Int?,
    highlights: Map<String, VerseHighlight>,
    bookId: String,
    chapterNumber: Int,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val paragraphText = remember(verses, config, selectedVerseNumber, highlights) {
        buildAnnotatedString {
            verses.forEachIndexed { index, verse ->
                val isSelected = verse.number == selectedVerseNumber
                val highlight = highlights["$bookId:$chapterNumber:${verse.number}"]

                pushStringAnnotation("VERSE", verse.number.toString())
                if (config.showVerseNumbers && verse.number > 0) {
                    appendVerseNumber(
                        verse = verse,
                        config = config,
                        highlight = highlight,
                        isSelected = isSelected,
                        inline = true,
                        primaryColor = primaryColor,
                        onSurfaceVariant = primaryContainer,
                    )
                }
                appendVerseBody(verse, config, highlight, isSelected)
                pop()

                if (index < verses.lastIndex) append(" ")
            }
        }
    }

    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = config.fontFamily,
        fontSize = config.fontSize.sp,
        lineHeight = (config.fontSize * config.lineHeight).sp,
        textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
        color = config.primaryTextColor.takeIf { it != Color.Unspecified } ?: onSurfaceColor,
    )

    Text(
        text = paragraphText,
        style = textStyle,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(paragraphText) {
                detectTapGestures(
                    onTap = { position ->
                        val l = layout ?: return@detectTapGestures
                        val charOffset = l.getOffsetForPosition(position)
                        val verse = paragraphText
                            .getStringAnnotations("VERSE", charOffset, charOffset)
                            .firstOrNull()
                            ?.item
                            ?.toIntOrNull()
                        verse?.let { onVerseTap?.invoke(it) }
                    },
                    onLongPress = { position ->
                        val l = layout ?: return@detectTapGestures
                        val charOffset = l.getOffsetForPosition(position)
                        val verse = paragraphText
                            .getStringAnnotations("VERSE", charOffset, charOffset)
                            .firstOrNull()
                            ?.item
                            ?.toIntOrNull()
                        verse?.let { onVerseLongPress?.invoke(it) }
                    },
                )
            },
        onTextLayout = { layout = it },
    )
}

// ── Annotation building (shared by both layouts) ──────────────────

/**
 * Appends a verse number token with its surrounding asterisk (if any).
 *
 * [primaryColor] and [onSurfaceVariant] are threaded in explicitly
 * because this is a plain (non-@Composable) builder — MaterialTheme
 * isn't reachable from inside `buildAnnotatedString {}`.
 */
private fun AnnotatedString.Builder.appendVerseNumber(
    verse: ReaderVerse,
    config: WordConfig,
    highlight: VerseHighlight?,
    isSelected: Boolean,
    inline: Boolean,
    primaryColor: Color,
    onSurfaceVariant: Color,
) {
    val resolvedColor = if (primaryColor == Color.Unspecified) {
        Color(0xFF6750A4) // M3 baseline primary
    } else primaryColor

    val bg = when {
        isSelected -> onSurfaceVariant // primaryContainer passed in
        highlight != null -> highlight.color.color.copy(alpha = 0.4f)
        else -> Color.Transparent
    }

    withStyle(
        SpanStyle(
            color = resolvedColor,
            fontWeight = FontWeight.Bold,
            fontSize = (config.fontSize * 0.65f).sp,
            background = bg,
        )
    ) {
        append("${verse.number}")
        if (config.showVerseNotes && verse.footnoteIds.isNotEmpty()) append("*")
        if (inline) append(" ")
    }
    if (!inline) append(" ")
}

private fun AnnotatedString.Builder.appendVerseBody(
    verse: ReaderVerse,
    config: WordConfig,
    highlight: VerseHighlight? = null,
    isSelected: Boolean = false,
) {
    val textBg = when {
        isSelected -> Color.Transparent // verse-number span already carries the selection bg
        highlight != null -> highlight.color.color.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    withStyle(SpanStyle(background = textBg)) {
        if (verse.words.isNotEmpty()) {
            appendVerseWords(verse, config)
        } else {
            append(verse.text)
        }
    }
}

// ── Per-word rendering ────────────────────────────────────────────

private fun AnnotatedString.Builder.appendVerseWords(
    verse: ReaderVerse,
    config: WordConfig,
) {
    val defaultRed = Color(0xFFB71C1C)
    val redLetter = if (config.redWordsColor == Color.Unspecified) {
        defaultRed
    } else config.redWordsColor

    verse.words.forEachIndexed { index, word ->
        val color = if (config.showRedWords && word.isWordsOfJesus) redLetter
        else Color.Unspecified

        withStyle(
            SpanStyle(
                color = color,
                fontWeight = if (config.showStrongs && word.strongs != null) FontWeight.Medium
                else FontWeight.Normal,
            )
        ) {
            append(word.text)
        }

        if (index < verse.words.lastIndex) {
            if (shouldSpace(word.text, verse.words[index + 1].text)) append(" ")
        }
    }
}

/**
 * Decide whether a space belongs between two consecutive word tokens.
 *
 * Returns `true` in every ordinary case (including after commas, periods,
 * colons, semicolons, and closing punctuation). Returns `false` only for
 * the three cases where a space would visibly look wrong:
 *
 *   1. Before closing punctuation  — `word)` not `word )`
 *   2. After opening punctuation   — `(word` not `( word`
 *   3. Before apostrophe-continuation — `God's` not `God 's`
 */
private fun shouldSpace(prev: String, next: String): Boolean {
    if (prev.isEmpty() || next.isEmpty()) return false
    if (prev.last().isWhitespace() || next.first().isWhitespace()) return false
    val noSpaceBefore = ",.;:!?)]}%"
    if (next.first() in noSpaceBefore) return false
    val noSpaceAfter = "([{"
    if (prev.last() in noSpaceAfter) return false
    if (next.first() == '\'' && next.length > 1) return false
    return true
}

// ── Chapter footer (In This Chapter) ──────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterFooter(
    chapter: ReaderChapter,
    entities: List<EntityRef>,
    onEntityTap: ((EntityRef) -> Unit)?,
) {
    val byKind = remember(entities) {
        entities.groupBy { it.kind.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.outlineVariant)
                        )
                    )
            )
            Text(
                text = "IN THIS CHAPTER",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                letterSpacing = 1.5.sp,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.outlineVariant, Color.Transparent)
                        )
                    )
            )
        }

        Spacer(Modifier.height(16.dp))

        byKind.forEach { (kind, refs) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = kindLabel(kind),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .width(52.dp),
                )
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    refs.forEach { entity ->
                        AssistChip(
                            onClick = { onEntityTap?.invoke(entity) },
                            label = {
                                Text(
                                    text = entity.name,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            leadingIcon = {
                                EntityAvatar(entity = entity, size = 18.dp)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                            border = null,
                        )
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: String): String = when (kind) {
    "person" -> "People"
    "place" -> "Places"
    "event" -> "Events"
    "group" -> "Groups"
    else -> "Other"
}