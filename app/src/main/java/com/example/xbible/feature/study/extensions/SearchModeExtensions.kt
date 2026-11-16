package com.example.xbible.feature.study.extensions

import androidx.annotation.StringRes
import com.example.xbible.R
import uniffi.xbible_engine.SearchMode

/**
 * Maps a [SearchMode] to the string resource that names it in the UI.
 *
 * The `when` is exhaustive on purpose: adding a new variant to
 * [SearchMode] in Rust causes a compile error here until the
 * extension is updated, so the UI can never show a missing label.
 */
@get:StringRes
val SearchMode.labelRes: Int
    get() = when (this) {
        SearchMode.MULTI_WORD -> R.string.search_mode_multi_word
        SearchMode.PHRASE -> R.string.search_mode_phrase
        SearchMode.REGEX -> R.string.search_mode_regex
    }