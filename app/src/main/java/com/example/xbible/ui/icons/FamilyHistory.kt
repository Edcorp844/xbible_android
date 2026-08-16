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
public val family_history: ImageVector
    get() {
        if (_family_history != null) {
            return _family_history!!
        }
        _family_history =
            ImageVector.Builder(
                name = "family_history",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.Companion.NonZero,
                    ) {
                        moveTo(9.34f, 21.41f)
                        quadTo(8.25f, 20.33f, 8.25f, 18.75f)
                        quadToRelative(0f, -1.3f, 0.78f, -2.29f)
                        quadTo(9.8f, 15.48f, 11f, 15.13f)
                        verticalLineTo(13f)
                        horizontalLineTo(5f)
                        verticalLineTo(9f)
                        horizontalLineTo(2.5f)
                        verticalLineTo(2f)
                        horizontalLineToRelative(7f)
                        verticalLineTo(9f)
                        horizontalLineTo(7f)
                        verticalLineToRelative(2f)
                        horizontalLineTo(17f)
                        verticalLineTo(8.88f)
                        quadTo(15.8f, 8.52f, 15.03f, 7.54f)
                        reflectiveQuadTo(14.25f, 5.25f)
                        quadToRelative(0f, -1.58f, 1.09f, -2.66f)
                        reflectiveQuadTo(18f, 1.5f)
                        reflectiveQuadToRelative(2.66f, 1.09f)
                        reflectiveQuadToRelative(1.09f, 2.66f)
                        quadToRelative(0f, 1.3f, -0.77f, 2.29f)
                        reflectiveQuadTo(19f, 8.88f)
                        verticalLineTo(13f)
                        horizontalLineTo(13f)
                        verticalLineToRelative(2.13f)
                        quadToRelative(1.2f, 0.35f, 1.98f, 1.34f)
                        reflectiveQuadToRelative(0.77f, 2.29f)
                        quadToRelative(0f, 1.57f, -1.09f, 2.66f)
                        reflectiveQuadTo(12f, 22.5f)
                        reflectiveQuadTo(9.34f, 21.41f)
                        close()
                        moveTo(19.24f, 6.49f)
                        quadTo(19.75f, 5.97f, 19.75f, 5.25f)
                        quadToRelative(0f, -0.73f, -0.51f, -1.24f)
                        reflectiveQuadTo(18f, 3.5f)
                        quadToRelative(-0.72f, 0f, -1.24f, 0.51f)
                        reflectiveQuadTo(16.25f, 5.25f)
                        quadToRelative(0f, 0.72f, 0.51f, 1.24f)
                        quadTo(17.28f, 7f, 18f, 7f)
                        quadToRelative(0.73f, 0f, 1.24f, -0.51f)
                        close()
                        moveTo(4.5f, 7f)
                        horizontalLineToRelative(3f)
                        verticalLineTo(4f)
                        horizontalLineToRelative(-3f)
                        verticalLineTo(7f)
                        close()
                        moveToRelative(8.74f, 12.99f)
                        quadToRelative(0.51f, -0.51f, 0.51f, -1.24f)
                        reflectiveQuadTo(13.24f, 17.51f)
                        reflectiveQuadTo(12f, 17f)
                        quadToRelative(-0.72f, 0f, -1.24f, 0.51f)
                        reflectiveQuadToRelative(-0.51f, 1.24f)
                        reflectiveQuadToRelative(0.51f, 1.24f)
                        reflectiveQuadTo(12f, 20.5f)
                        reflectiveQuadToRelative(1.24f, -0.51f)
                        close()
                        moveTo(6f, 5.5f)
                        close()
                        moveTo(18f, 5.25f)
                        close()
                        moveToRelative(-6f, 13.5f)
                        close()
                    }
                }
                .build()
        return _family_history!!
    }

private var _family_history: ImageVector? = null