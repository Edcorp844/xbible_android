package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")

internal val Sidebar: ImageVector
    get() {
        if (_sidebar != null) {
            return _sidebar!!
        }
        _sidebar =
            ImageVector.Builder(
                name = "Sierra",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(2f, 11f)
                        curveTo(2f, 7.22876f, 2f, 5.34315f, 3.17157f, 4.17157f)
                        curveTo(4.34315f, 3f, 6.22876f, 3f, 10f, 3f)
                        lineTo(14f, 3f)
                        curveTo(17.7712f, 3f, 19.6569f, 3f, 20.8284f, 4.17157f)
                        curveTo(22f, 5.34315f, 22f, 7.22876f, 22f, 11f)
                        lineTo(22f, 13f)
                        curveTo(22f, 16.7712f, 22f, 18.6569f, 20.8284f, 19.8284f)
                        curveTo(19.6569f, 21f, 17.7712f, 21f, 14f, 21f)
                        lineTo(10f, 21f)
                        curveTo(6.22876f, 21f, 4.34315f, 21f, 3.17157f, 19.8284f)
                        curveTo(2f, 18.6569f, 2f, 16.7712f, 2f, 13f)
                        lineTo(2f, 11f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(15f, 21f)
                        lineTo(15f, 3f)
                    }
                }
                .build()
        return _sidebar!!
    }

private var _sidebar: ImageVector? = null