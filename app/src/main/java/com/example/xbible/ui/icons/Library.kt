package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")

internal val Library: ImageVector
    get() {
        if (_library != null) {
            return _library!!
        }
        _library =
            ImageVector.Builder(
                name = "PowerBank",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    // Path 1: Top horizontal band arch
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(19.5617f, 7f)
                        curveTo(19.7904f, 5.6952f, 18.7863f, 4.5f, 17.4617f, 4.5f)
                        horizontalLineTo(6.5379f)
                        curveTo(5.2132f, 4.5f, 4.2092f, 5.6952f, 4.4378f, 7f)
                    }
                    // Path 2: Top connector cap
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(17.4999f, 4.5f)
                        curveTo(17.5283f, 4.2409f, 17.5425f, 4.1114f, 17.5427f, 4.0044f)
                        curveTo(17.545f, 2.9807f, 16.7739f, 2.1206f, 15.7561f, 2.0114f)
                        curveTo(15.6497f, 2f, 15.5194f, 2f, 15.2588f, 2f)
                        horizontalLineTo(8.741f)
                        curveTo(8.4804f, 2f, 8.35f, 2f, 8.2436f, 2.0114f)
                        curveTo(7.2258f, 2.1206f, 6.4548f, 2.9807f, 6.457f, 4.0043f)
                        curveTo(6.4573f, 4.1114f, 6.4715f, 4.2409f, 6.4998f, 4.5f)
                    }
                    // Path 3: Center indicator line
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(15f, 18f)
                        horizontalLineTo(9f)
                    }
                    // Path 4: Main body outline
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(2.3835f, 13.793f)
                        curveTo(1.9375f, 10.6294f, 1.7145f, 9.0477f, 2.6623f, 8.0238f)
                        curveTo(3.6102f, 7f, 5.2976f, 7f, 8.6724f, 7f)
                        horizontalLineTo(15.3276f)
                        curveTo(18.7024f, 7f, 20.3898f, 7f, 21.3377f, 8.0238f)
                        curveTo(22.2855f, 9.0477f, 22.0625f, 10.6294f, 21.6165f, 13.793f)
                        lineTo(21.1935f, 16.793f)
                        curveTo(20.8437f, 19.2739f, 20.6689f, 20.5143f, 19.7717f, 21.2572f)
                        curveTo(18.8745f, 22f, 17.5512f, 22f, 14.9046f, 22f)
                        horizontalLineTo(9.0954f)
                        curveTo(6.4488f, 22f, 5.1255f, 22f, 4.2283f, 21.2572f)
                        curveTo(3.3312f, 20.5143f, 3.1563f, 19.2739f, 2.8065f, 16.793f)
                        lineTo(2.3835f, 13.793f)
                        close()
                    }
                }
                .build()
        return _library!!
    }

private var _library: ImageVector? = null