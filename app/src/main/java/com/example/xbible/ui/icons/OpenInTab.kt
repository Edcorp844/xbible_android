package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")

internal val OpenInTab: ImageVector
    get() {
        if (_openInTab != null) {
            return _openInTab!!
        }
        _openInTab =
            ImageVector.Builder(
                name = "OpenInTab",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(13f, 11f)
                        lineTo(22f, 2f)
                        moveTo(22f, 2f)
                        lineTo(16.6562f, 2f)
                        moveTo(22f, 2f)
                        lineTo(22f, 7.34375f)
                    }
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(22f, 12f)
                        curveTo(22f, 16.714f, 22f, 19.0711f, 20.5355f, 20.5355f)
                        curveTo(19.0711f, 22f, 16.714f, 22f, 12f, 22f)
                        curveTo(7.28595f, 22f, 4.92893f, 22f, 3.46447f, 20.5355f)
                        curveTo(2f, 19.0711f, 2f, 16.714f, 2f, 12f)
                        curveTo(2f, 7.28595f, 2f, 4.92893f, 3.46447f, 3.46447f)
                        curveTo(4.92893f, 2f, 7.28595f, 2f, 12f, 2f)
                    }
                }
                .build()
        return _openInTab!!
    }

private var _openInTab: ImageVector? = null