package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val Lexicon: ImageVector
    get() {
        if (_lexicon != null) {
            return _lexicon!!
        }
        _lexicon =
            ImageVector.Builder(
                name = "Lexicon",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    // Continuous Outer Book Border Frame
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        // Outer Cover Contour
                        moveTo(10f, 2f)
                        horizontalLineTo(14f)
                        curveTo(16.8284f, 2f, 18.2426f, 2f, 19.1213f, 2.8787f)
                        curveTo(20f, 3.7574f, 20f, 5.1716f, 20f, 8f)
                        verticalLineTo(16f)
                        curveTo(20f, 18.8284f, 20f, 20.2426f, 19.1213f, 21.1213f)
                        curveTo(18.2426f, 22f, 16.8284f, 22f, 14f, 22f)
                        horizontalLineTo(10f)
                        curveTo(7.1716f, 22f, 5.7574f, 22f, 4.8787f, 21.1213f)
                        curveTo(4f, 20.2426f, 4f, 18.8284f, 4f, 16f)
                        verticalLineTo(8f)
                        curveTo(4f, 5.1716f, 4f, 3.7574f, 4.8787f, 2.8787f)
                        curveTo(5.7574f, 2f, 7.1716f, 2f, 10f, 2f)
                        close()
                    }

                    // Bottom Shelf / Page Ridge Baseline
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(19.8978f, 16f)
                        horizontalLineTo(7.8978f)
                        curveTo(6.9678f, 16f, 6.5028f, 16f, 6.1213f, 16.1022f)
                        curveTo(5.086f, 16.3796f, 4.2774f, 17.1883f, 4f, 18.2235f)
                    }

                    // Scaled & Centered Hebrew Aleph on the Front Cover
                    path(
                        fill = SolidColor(Color(0xFF1C274C)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(17.364f, 11.9589f)
                        lineTo(16.9035f, 12f)
                        curveTo(15.0175f, 10.0903f, 13.0439f, 8.2628f, 11.0482f, 6.4559f)
                        curveTo(10.7412f, 7.1335f, 10.6316f, 7.5441f, 10.6316f, 7.7084f)
                        curveTo(10.6316f, 7.9548f, 10.6535f, 8.1396f, 11.6623f, 10.152f)
                        lineTo(11.4211f, 11.7947f)
                        lineTo(9.2719f, 11.7947f)
                        curveTo(8.8114f, 11.7947f, 8.7456f, 11.7947f, 8.5702f, 11.9589f)
                        lineTo(8f, 11.8563f)
                        lineTo(8.3289f, 10.4805f)
                        curveTo(8.4167f, 10.0903f, 8.5263f, 10.0903f, 9.7544f, 10.0903f)
                        lineTo(10.2588f, 10.0903f)
                        curveTo(9.5351f, 8.5092f, 9.5351f, 8.386f, 9.5351f, 7.7084f)
                        curveTo(9.5351f, 7.4727f, 9.7325f, 6.9388f, 10.4781f, 5.5425f)
                        curveTo(9.1184f, 4.3721f, 8.9211f, 4.2078f, 8.9649f, 3.8177f)
                        lineTo(9.25f, 1.6f)
                        lineTo(9.9079f, 1.6411f)
                        curveTo(9.9079f, 2.1133f, 10.0395f, 2.6062f, 11.136f, 3.5713f)
                        curveTo(12.1667f, 4.4747f, 13.1535f, 5.3577f, 14.1623f, 6.3023f)
                        lineTo(15.3684f, 4.0025f)
                        lineTo(15.3246f, 4.0025f)
                        curveTo(14.557f, 4.0025f, 14.2061f, 3.8793f, 14.2719f, 3.2427f)
                        lineTo(14.4693f, 1.6f)
                        lineTo(15.1053f, 1.6411f)
                        curveTo(15.1053f, 2.0723f, 15.3904f, 2.216f, 16.114f, 2.216f)
                        lineTo(18f, 2.216f)
                        lineTo(17.7368f, 3.9409f)
                        curveTo(16.3772f, 3.9614f, 15.9825f, 4.4747f, 14.7982f, 6.8567f)
                        curveTo(15.7851f, 7.7807f, 16.7281f, 8.7047f, 17.6711f, 9.6903f)
                        lineTo(17.364f, 11.9589f)
                        close()
                    }
                }
                .build()
        return _lexicon!!
    }

private var _lexicon: ImageVector? = null