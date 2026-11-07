package com.example.xbible.feature.store

import uniffi.xbible_engine.StoreItem

/**
 * Store-internal navigation routes. Kept out of [AppDestinations] so they can
 * carry parameters (shelf title, item id/kind, etc.).
 */
sealed interface StoreRoute {

    /** "See All" from a shelf — flat list of that shelf's items. */
    data class Section(val title: String) : StoreRoute

    /** Item detail — every field of a single [StoreItem]. */
    data class ItemDetail(val id: String, val kind: String) : StoreRoute

    /** "See all languages" — full language browser. */
    data object Languages : StoreRoute

    /** All translations for a single language, filtered from the catalog. */
    data class Language(val code: String) : StoreRoute
}