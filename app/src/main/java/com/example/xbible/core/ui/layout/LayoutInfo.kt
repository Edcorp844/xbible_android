package com.example.xbible.core.ui.layout

import android.content.res.Configuration
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

enum class LayoutMode {
    PhonePortrait,
    PhoneLandscape,
    TabletPortrait,
    TabletLandscape,
}

data class LayoutInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val mode: LayoutMode,
) {
    val isCompactWidth: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact
    val isMediumWidth: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium
    val isExpandedWidth: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded

    val isTabletOrFoldable: Boolean
        get() = widthSizeClass != WindowWidthSizeClass.Compact

    val isLandscape: Boolean
        get() = mode == LayoutMode.PhoneLandscape || mode == LayoutMode.TabletLandscape

    val isCompactHeight: Boolean
        get() = heightSizeClass == WindowHeightSizeClass.Compact

    val supportsTwoPane: Boolean
        get() = mode == LayoutMode.TabletLandscape ||
                (mode == LayoutMode.TabletPortrait && isTabletOrFoldable)
}

val LocalLayoutInfo = staticCompositionLocalOf<LayoutInfo> {
    error("LocalLayoutInfo not provided — wrap your root in ProvideLayoutInfo { … }")
}

@Composable
fun ProvideLayoutInfo(
    windowSizeClass: WindowSizeClass,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscapeNow =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val mode = when {
        // Narrow + not rotated → phone portrait.
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && !isLandscapeNow ->
            LayoutMode.PhonePortrait

        // Landscape AND short → phone-style landscape, regardless of width.
        // This catches modern phones whose landscape width reports `Expanded`.
        isLandscapeNow && windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact ->
            LayoutMode.PhoneLandscape

        // Landscape with breathing room → tablet landscape.
        isLandscapeNow ->
            LayoutMode.TabletLandscape

        // Medium/expanded width, portrait → tablet portrait.
        else ->
            LayoutMode.TabletPortrait
    }

    val info = LayoutInfo(
        widthSizeClass = windowSizeClass.widthSizeClass,
        heightSizeClass = windowSizeClass.heightSizeClass,
        mode = mode,
    )

    CompositionLocalProvider(LocalLayoutInfo provides info, content = content)
}

@Composable
@ReadOnlyComposable
fun layoutInfo(): LayoutInfo = LocalLayoutInfo.current