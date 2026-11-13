package com.example.xbible.feature.study.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

private val BOOK_ALIASES: Map<String, String> = mapOf(
    "gen" to "GEN", "genesis" to "GEN",
    "ex" to "EXO", "exo" to "EXO", "exod" to "EXO", "exodus" to "EXO",
    "lev" to "LEV", "leviticus" to "LEV",
    "num" to "NUM", "numbers" to "NUM",
    "deut" to "DEU", "dt" to "DEU", "deuteronomy" to "DEU",
    "josh" to "JOS", "joshua" to "JOS",
    "judg" to "JDG", "judges" to "JDG",
    "ruth" to "RUT",
    "1sam" to "1SA", "1samuel" to "1SA", "1sa" to "1SA",
    "2sam" to "2SA", "2samuel" to "2SA", "2sa" to "2SA",
    "1kgs" to "1KI", "1kings" to "1KI", "1ki" to "1KI",
    "2kgs" to "2KI", "2kings" to "2KI", "2ki" to "2KI",
    "1chr" to "1CH", "1chronicles" to "1CH", "1ch" to "1CH",
    "2chr" to "2CH", "2chronicles" to "2CH", "2ch" to "2CH",
    "ezra" to "EZR",
    "neh" to "NEH", "nehemiah" to "NEH",
    "esth" to "EST", "esther" to "EST",
    "job" to "JOB",
    "ps" to "PSA", "psa" to "PSA", "psalm" to "PSA", "psalms" to "PSA",
    "prov" to "PRO", "proverbs" to "PRO", "pr" to "PRO",
    "eccl" to "ECC", "ecclesiastes" to "ECC", "ecc" to "ECC",
    "song" to "SNG", "songofsolomon" to "SNG", "sos" to "SNG", "sng" to "SNG",
    "isa" to "ISA", "isaiah" to "ISA",
    "jer" to "JER", "jeremiah" to "JER",
    "lam" to "LAM", "lamentations" to "LAM",
    "ezek" to "EZK", "ezekiel" to "EZK", "eze" to "EZK",
    "dan" to "DAN", "daniel" to "DAN",
    "hos" to "HOS", "hosea" to "HOS",
    "joel" to "JOL",
    "amos" to "AMO",
    "obad" to "OBA", "obadiah" to "OBA",
    "jonah" to "JON",
    "mic" to "MIC", "micah" to "MIC",
    "nah" to "NAM", "nahum" to "NAM",
    "hab" to "HAB", "habakkuk" to "HAB",
    "zeph" to "ZEP", "zephaniah" to "ZEP",
    "hag" to "HAG", "haggai" to "HAG",
    "zech" to "ZEC", "zechariah" to "ZEC",
    "mal" to "MAL", "malachi" to "MAL",
    "matt" to "MAT", "matthew" to "MAT", "mt" to "MAT",
    "mark" to "MRK", "mk" to "MRK",
    "luke" to "LUK", "lk" to "LUK",
    "john" to "JHN", "jn" to "JHN",
    "acts" to "ACT", "ac" to "ACT",
    "rom" to "ROM", "romans" to "ROM",
    "1cor" to "1CO", "1corinthians" to "1CO",
    "2cor" to "2CO", "2corinthians" to "2CO",
    "gal" to "GAL", "galatians" to "GAL",
    "eph" to "EPH", "ephesians" to "EPH",
    "phil" to "PHP", "philippians" to "PHP", "php" to "PHP",
    "col" to "COL", "colossians" to "COL",
    "1thess" to "1TH", "1thessalonians" to "1TH", "1th" to "1TH",
    "2thess" to "2TH", "2thessalonians" to "2TH", "2th" to "2TH",
    "1tim" to "1TI", "1timothy" to "1TI", "1ti" to "1TI",
    "2tim" to "2TI", "2timothy" to "2TI", "2ti" to "2TI",
    "titus" to "TIT", "tit" to "TIT",
    "phlm" to "PHM", "philemon" to "PHM",
    "heb" to "HEB", "hebrews" to "HEB",
    "jas" to "JAS", "james" to "JAS",
    "1pet" to "1PE", "1peter" to "1PE", "1pe" to "1PE",
    "2pet" to "2PE", "2peter" to "2PE", "2pe" to "2PE",
    "1john" to "1JN", "1jn" to "1JN",
    "2john" to "2JN", "2jn" to "2JN",
    "3john" to "3JN", "3jn" to "3JN",
    "jude" to "JUD",
    "rev" to "REV", "revelation" to "REV",
)

private val REFERENCE_REGEX = Regex(
    """\b((?:[1-3]\s*)?[A-Z][a-zA-Z]+)\.?\s+(\d{1,3})(?::(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?)?"""
)

private data class ScriptureRef(
    val raw: String,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int?,
    val verseEnd: Int?,
)

private fun findReferences(text: String): List<ScriptureRef> {
    val out = mutableListOf<ScriptureRef>()
    for (match in REFERENCE_REGEX.findAll(text)) {
        val bookToken = match.groupValues[1]
            .lowercase()
            .replace(" ", "")
            .replace(".", "")
        val bookId = BOOK_ALIASES[bookToken] ?: continue
        val chapter = match.groupValues[2].toIntOrNull() ?: continue
        val vs = match.groupValues[3].toIntOrNull()
        val ve = match.groupValues[4].toIntOrNull()
        out += ScriptureRef(
            raw = match.value,
            bookId = bookId,
            chapter = chapter,
            verseStart = vs,
            verseEnd = ve,
        )
    }
    return out
}

@Composable
fun ScriptureReferenceText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified, // Explicitly added to match PageView behavior
    onReferenceClick: (bookId: String, chapter: Int, verseStart: Int?, verseEnd: Int?) -> Unit = { _, _, _, _ -> },
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val annotated: AnnotatedString = remember(text, primaryColor) {
        val refs = findReferences(text)
        buildAnnotatedString {
            var cursor = 0
            for (ref in refs) {
                val start = text.indexOf(ref.raw, startIndex = cursor)
                if (start < 0) continue
                append(text.substring(cursor, start))
                pushStringAnnotation(
                    tag = "REF",
                    annotation = "${ref.bookId}:${ref.chapter}:${ref.verseStart ?: -1}:${ref.verseEnd ?: -1}",
                )
                withStyle(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline,
                    )
                ) {
                    append(ref.raw)
                }
                pop()
                cursor = start + ref.raw.length
            }
            append(text.substring(cursor))
        }
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = style,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(annotated) {
                detectTapGestures { offset ->
                    val l = layout ?: return@detectTapGestures
                    val charOffset = l.getOffsetForPosition(offset)
                    annotated.getStringAnnotations("REF", charOffset, charOffset)
                        .firstOrNull()
                        ?.let { ann ->
                            val parts = ann.item.split(":")
                            if (parts.size == 4) {
                                onReferenceClick(
                                    parts[0],
                                    parts[1].toIntOrNull() ?: return@let,
                                    parts[2].toIntOrNull()?.takeIf { it > 0 },
                                    parts[3].toIntOrNull()?.takeIf { it > 0 },
                                )
                            }
                        }
                }
            },
        onTextLayout = { layout = it },
    )
}