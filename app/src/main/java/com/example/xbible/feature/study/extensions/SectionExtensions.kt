package com.example.xbible.feature.study.extensions

import androidx.annotation.StringRes
import com.example.xbible.R

/**
 * A canonical section key as stored in `book_metadata.section`.
 *
 * Closed set — the keys are fixed by `install::section_for_book_id`
 * on the Rust side. The `when` in [labelRes] is exhaustive so a
 * missing label is a compile error.
 */
enum class Section(val wire: String) {
    Ot("ot"),
    Nt("nt"),
    Deutero("deutero"),
    OrthodoxExtra("orthodox_extra"),
    EthiopianExtra("ethiopian_extra"),
    Unknown("unknown");

    companion object {
        /// Parse the wire value. Unknown values land in [Unknown] so
        /// the UI still has a label to render.
        fun fromWire(s: String): Section =
            entries.firstOrNull { it.wire == s } ?: Unknown

        /// Default "everything visible" set for a new tab. Matches
        /// "no filter" — every section the translation might contain
        /// is selected.
        val All: Set<Section> = entries.toSet()
    }
}

@get:StringRes
val Section.labelRes: Int
    get() = when (this) {
        Section.Ot -> R.string.section_ot
        Section.Nt -> R.string.section_nt
        Section.Deutero -> R.string.section_deutero
        Section.OrthodoxExtra -> R.string.section_orthodox_extra
        Section.EthiopianExtra -> R.string.section_ethiopian_extra
        Section.Unknown -> R.string.section_unknown
    }