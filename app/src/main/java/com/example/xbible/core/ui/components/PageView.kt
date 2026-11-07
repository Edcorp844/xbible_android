package com.example.xbible.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
    chapterEntities: List<EntityRef> = emptyList(),
    onEntityTap: ((EntityRef) -> Unit)? = null,
    selectedVerseNumber: Int? = null,
    highlights: Map<String, VerseHighlight> = emptyMap(),
) {
    // 1. Group the blocks into logical sections (Heading + Paragraphs)
    val sections = remember(chapter) { groupBlocksIntoSections(chapter.blocks) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        state = state,
        // Changed horizontal padding to 16.dp to remove the large gutter
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item(key = "chapter-header") {
            ChapterHeader(chapter, config)
        }

        // Render grouped sections
        items(sections, key = { it.id }) { section ->
            ParagraphSectionView(
                section = section,
                config = config,
                onStrongsClick = onStrongsClick,
                onVerseTap = onVerseTap,
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

            "verse" -> {
                block.verses.forEach { currentVerses.add(it) }
            }
            // Ignore line breaks so verses flow seamlessly
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
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = config.fontFamily,
            ),
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
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    )
            )

            Text(
                text = chapter.chapterNumber.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = config.fontFamily,
                    fontWeight = FontWeight.Bold
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
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ── Section Renderer ──────────────────────────────────────────────

@Composable
private fun ParagraphSectionView(
    section: ParagraphSection,
    config: WordConfig,
    onStrongsClick: ((String) -> Unit)?,
    onVerseTap: ((Int) -> Unit)?,
    selectedVerseNumber: Int?,
    highlights: Map<String, VerseHighlight> = emptyMap(),
    bookId: String = "",
    chapterNumber: Int = 1,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        // Render H2 Heading if present
        section.headingText?.let { heading ->
            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = config.fontFamily,
                    fontSize = config.fontSize.sp * 1.025,
                    lineHeight = (config.fontSize * config.lineHeight).sp * 1.025,
                    textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    top = 16.dp,
                    bottom = if (section.subtitleText == null) {
                        4.dp
                    } else {
                        12.dp
                    }
                ),
            )
        }

        // Render H3 Subtitle if present
        section.subtitleText?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = config.fontFamily,
                    fontSize = config.fontSize.sp * 0.8,
                    lineHeight = (config.fontSize * config.lineHeight).sp * 0.8,
                    textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                ),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = if (section.headingText == null) {
                        0.dp
                    } else {
                        8.dp
                    }, bottom = 8.dp
                ),
            )
        }

        // Render verses based on layout
        if (section.verses.isNotEmpty()) {
            if (config.verseLayout == VerseLayout.VersePerLine) {
                val verseSpacing =
                    (config.fontSize * (config.verseSpacing)).dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(verseSpacing)
                ) {
                    section.verses.forEach { verse ->
                        val isSelected = verse.number == selectedVerseNumber
                        val highlightKey = "$bookId:$chapterNumber:${verse.number}"
                        val highlight = highlights[highlightKey]

                        val backgroundModifier = when {
                            isSelected -> Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.4f
                                ), RoundedCornerShape(8.dp)
                            )

                            highlight != null && highlight.style == HighlightStyle.Solid -> Modifier.background(
                                highlight.color.color.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )

                            highlight != null && highlight.style == HighlightStyle.Marker -> Modifier.background(
                                highlight.color.color.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )

                            else -> Modifier.background(Color.Transparent)
                        }

                        val underlineModifier =
                            if (highlight != null && highlight.style == HighlightStyle.Underline) {
                                Modifier.drawBehind {
                                    drawLine(
                                        color = highlight.color.color,
                                        start = androidx.compose.ui.geometry.Offset(
                                            0f,
                                            size.height
                                        ),
                                        end = androidx.compose.ui.geometry.Offset(
                                            size.width,
                                            size.height
                                        ),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                            } else Modifier

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(backgroundModifier)
                                .then(underlineModifier)
                                .clickable { onVerseTap?.invoke(verse.number) }
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            if (config.showVerseNumbers && verse.number > 0) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(
                                        text = "${verse.number}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (config.showVerseNotes && verse.footnoteIds.isNotEmpty()) {
                                        Text(
                                            text = "*",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(start = 1.dp),
                                        )
                                    }
                                }
                            }
                            val verseText = buildAnnotatedString {
                                if (verse.words.isNotEmpty()) {
                                    appendVerseWords(verse, config, onStrongsClick)
                                } else {
                                    append(verse.text)
                                }
                            }
                            Text(
                                text = verseText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = config.fontFamily,
                                    fontSize = config.fontSize.sp,
                                    lineHeight = (config.fontSize * config.lineHeight).sp,
                                    textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                                ),
                                color = if (config.primaryTextColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else config.primaryTextColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else {
                // Paragraph Mode
                val paragraphText = buildAnnotatedString {
                    section.verses.forEachIndexed { index, verse ->
                        val isSelected = verse.number == selectedVerseNumber
                        val highlightKey = "$bookId:$chapterNumber:${verse.number}"
                        val highlight = highlights[highlightKey]

                        pushStringAnnotation(tag = "VERSE", annotation = verse.number.toString())

                        // Append verse number
                        if (config.showVerseNumbers && verse.number > 0) {
                            val bg = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                highlight != null -> highlight.color.color.copy(alpha = 0.4f)
                                else -> Color.Transparent
                            }
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (config.fontSize * 0.65f).sp,
                                    background = bg,
                                )
                            ) {
                                append("${verse.number}")
                                if (config.showVerseNotes && verse.footnoteIds.isNotEmpty()) {
                                    append("*")
                                }
                                append(" ")
                            }
                        }

                        // Append verse words/text
                        val textBg = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            highlight != null -> highlight.color.color.copy(alpha = 0.35f)
                            else -> Color.Transparent
                        }
                        if (verse.words.isNotEmpty()) {
                            withStyle(SpanStyle(background = textBg)) {
                                appendVerseWords(verse, config, onStrongsClick)
                            }
                        } else {
                            withStyle(SpanStyle(background = textBg)) {
                                append(verse.text)
                            }
                        }
                        pop()

                        // Add a space between verses so they don't stick together
                        if (index < section.verses.lastIndex) {
                            append(" ")
                        }
                    }
                }

                ClickableText(
                    text = paragraphText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = config.fontFamily,
                        fontSize = config.fontSize.sp,
                        lineHeight = (config.fontSize * config.lineHeight).sp,
                        textAlign = if (config.justify) TextAlign.Justify else TextAlign.Start,
                        color = if (config.primaryTextColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else config.primaryTextColor,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { offset ->
                        paragraphText.getStringAnnotations(
                            tag = "VERSE",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                val verseNum = annotation.item.toIntOrNull()
                                if (verseNum != null) {
                                    onVerseTap?.invoke(verseNum)
                                }
                            }
                    }
                )
            }
        }
    }
}

// ── Per-word rendering ────────────────────────────────────────────

private fun AnnotatedString.Builder.appendVerseWords(
    verse: ReaderVerse,
    config: WordConfig,
    onStrongsClick: ((String) -> Unit)?,
) {
    val defaultRed = Color(0xFFB71C1C)
    val redLetter =
        if (config.redWordsColor == Color.Unspecified) defaultRed else config.redWordsColor

    verse.words.forEachIndexed { index, word ->
        val color = if (config.showRedWords && word.isWordsOfJesus)
            redLetter else Color.Unspecified

        withStyle(
            SpanStyle(
                color = color,
                fontWeight = if (config.showStrongs && word.strongs != null) FontWeight.Medium else FontWeight.Normal,
            )
        ) {
            append(word.text)
        }

        if (index < verse.words.lastIndex) {
            if (shouldSpace(word.text, verse.words[index + 1].text)) {
                append(" ")
            }
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
 *
 * If either token already carries whitespace at the boundary, we do not
 * add another space (the tokenizer already spaced it).
 */
private fun shouldSpace(prev: String, next: String): Boolean {
    if (prev.isEmpty() || next.isEmpty()) return false

    // Already spaced at the boundary — don't double up.
    if (prev.last().isWhitespace() || next.first().isWhitespace()) return false

    // Never a space *before* a closer.
    val noSpaceBefore = ",.;:!?)]}%"
    if (next.first() in noSpaceBefore) return false

    // Never a space *after* an opener.
    val noSpaceAfter = "([{"
    if (prev.last() in noSpaceAfter) return false

    // Never a space before a possessive/contraction apostrophe
    // ("God's", "don't", "he'll").
    if (next.first() == '\'' && next.length > 1) return false

    return true
}

// ── Chapter Footer (In This Chapter) ──────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChapterFooter(
    chapter: ReaderChapter,
    entities: List<EntityRef>,
    onEntityTap: ((EntityRef) -> Unit)?,
) {
    // Group by kind so People / Places / Events read as separate lines,
    // the way Doxa separates "who is in the passage" from "what it is about".
    val byKind = remember(entities) {
        entities.groupBy { it.kind.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // ── Section label row ───────────────────────────────────
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
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.outlineVariant
                            )
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
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── One row per kind: label on the left, chips on the right ──
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
                                EntityAvatar(
                                    entity = entity,
                                    size = 18.dp,
                                )
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