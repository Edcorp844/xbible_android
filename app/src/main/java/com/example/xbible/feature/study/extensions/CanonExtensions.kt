package com.example.xbible.feature.study.extensions

import androidx.annotation.StringRes
import com.example.xbible.R

/**
 * The canonical family a translation is showing. Mirrors the string
 * keys the Rust side stores in `translations.selected_canon`.
 *
 * Closed set — adding a canon is a Rust migration plus a new case
 * here plus a new string resource. The `when` in [labelRes] is
 * exhaustive so the compiler catches a missing label.
 */
enum class Canon(val wire: String) {
    Protestant("protestant"),
    Catholic("catholic"),
    Orthodox("orthodox"),
    Ethiopian("ethiopian"),
    All("all");

    companion object {
        /// Parse the wire value stored in the DB. Unknown values fall
        /// back to [Protestant], matching the Rust default.
        fun fromWire(s: String): Canon =
            entries.firstOrNull { it.wire == s } ?: Protestant
    }
}

@get:StringRes
val Canon.labelRes: Int
    get() = when (this) {
        Canon.Protestant -> R.string.canon_protestant
        Canon.Catholic -> R.string.canon_catholic
        Canon.Orthodox -> R.string.canon_orthodox
        Canon.Ethiopian -> R.string.canon_ethiopian
        Canon.All -> R.string.canon_all
    }