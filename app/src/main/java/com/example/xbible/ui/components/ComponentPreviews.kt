package com.example.xbible.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.xbible.ui.theme.XbibleTheme
import uniffi.xbible_engine.LexicalInfo
import uniffi.xbible_engine.Section
import uniffi.xbible_engine.TextDirection
import uniffi.xbible_engine.Verse
import uniffi.xbible_engine.Word

@Preview(showBackground = true)
@Composable
fun PageViewPreview() {
    XbibleTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val sampleSections = listOf(
                Section(
                    title = listOf(
                        createMockWord("The", isBoldText = true),
                        createMockWord("Creation", isBoldText = true),
                        createMockWord("of", isBoldText = true),
                        createMockWord("Light", isBoldText = true)
                    ),
                    verses = listOf(
                        Verse("gen.1.1", 1, listOf(
                            createMockWord("In", strongs = listOf("H7225"), morph = listOf("prep")),
                            createMockWord("the", strongs = listOf("H853")),
                            createMockWord("beginning", strongs = listOf("H7225"), morph = listOf("ncfsa")),
                            createMockWord("God", isBoldText = true, strongs = listOf("H430"), morph = listOf("ncmpa")),
                            createMockWord("created", strongs = listOf("H1254"), morph = listOf("vqp3ms")),
                            createMockWord("the", strongs = listOf("H853")),
                            createMockWord("heavens", strongs = listOf("H8064"), morph = listOf("ncmpb")),
                            createMockWord("and", strongs = listOf("H900")),
                            createMockWord("the", strongs = listOf("H853")),
                            createMockWord("earth.", strongs = listOf("H776"), morph = listOf("ncfsb"))
                        ), emptyList(), false),
                        Verse("gen.1.2", 2, listOf(
                            createMockWord("The", strongs = listOf("H776")),
                            createMockWord("earth", strongs = listOf("H776"), morph = listOf("ncfs")),
                            createMockWord("was", isItalic = true, strongs = listOf("H1961")),
                            createMockWord("without", strongs = listOf("H8414"), morph = listOf("ncmsa")),
                            createMockWord("form,", strongs = listOf("H8414")),
                            createMockWord("and", strongs = listOf("H900")),
                            createMockWord("void;", strongs = listOf("H922"), morph = listOf("ncmsa")),
                            createMockWord("and", strongs = listOf("H900")),
                            createMockWord("darkness", strongs = listOf("H2822"), morph = listOf("ncmsa")),
                            createMockWord("was", isItalic = true),
                            createMockWord("on", strongs = listOf("H5921")),
                            createMockWord("the", strongs = listOf("H6440")),
                            createMockWord("face", strongs = listOf("H6440"), morph = listOf("ncmpc")),
                            createMockWord("of", strongs = listOf("H6440")),
                            createMockWord("the", strongs = listOf("H8415")),
                            createMockWord("deep.", strongs = listOf("H8415"), morph = listOf("ncmsa"))
                        ), emptyList(), false)
                    ),
                    textDirection = TextDirection.LTR
                )
            )

            PageView(sections = sampleSections)
        }
    }
}

private fun createMockWord(
    text: String,
    isRed: Boolean = false,
    isItalic: Boolean = false,
    isBoldText: Boolean = false,
    strongs: List<String> = emptyList(),
    morph: List<String> = emptyList()
): Word = Word(
    text = text,
    isRed = isRed,
    isItalic = isItalic,
    isBoldText = isBoldText,
    lex = if (strongs.isNotEmpty() || morph.isNotEmpty()) LexicalInfo(strongs, null, null, morph) else null,
    note = null,
    isFirstInGroup = false,
    isLastInGroup = false,
    isPunctuation = false,
    isTitle = false,
    language = "en"
)
