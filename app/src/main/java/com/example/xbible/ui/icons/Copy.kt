package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")

internal val Copy: ImageVector
    get() {
        if (_copy != null) {
            return _copy!!
        }
        _copy =
            ImageVector.Builder(
                name = "Copy",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color(0xFF1C274C)),
                        pathFillType = PathFillType.EvenOdd,
                    ) {
                        moveTo(15f, 1.25f)
                        horizontalLineTo(10.9436f)
                        curveTo(9.1058f, 1.25f, 7.6502f, 1.25f, 6.511f, 1.4031f)
                        curveTo(5.3386f, 1.5608f, 4.3896f, 1.8929f, 3.6412f, 2.6412f)
                        curveTo(2.8929f, 3.3896f, 2.5608f, 4.3386f, 2.4031f, 5.511f)
                        curveTo(2.25f, 6.6502f, 2.25f, 8.1058f, 2.25f, 9.9436f)
                        verticalLineTo(16f)
                        curveTo(2.25f, 17.8722f, 3.6221f, 19.424f, 5.4155f, 19.7047f)
                        curveTo(5.5535f, 20.4687f, 5.8175f, 21.1208f, 6.3484f, 21.6517f)
                        curveTo(6.9503f, 22.2536f, 7.7081f, 22.5125f, 8.6083f, 22.6335f)
                        curveTo(9.4752f, 22.75f, 10.5775f, 22.75f, 11.9451f, 22.75f)
                        horizontalLineTo(15.0549f)
                        curveTo(16.4225f, 22.75f, 17.5248f, 22.75f, 18.3918f, 22.6335f)
                        curveTo(19.2919f, 22.5125f, 20.0497f, 22.2536f, 20.6517f, 21.6517f)
                        curveTo(21.2536f, 21.0497f, 21.5125f, 20.2919f, 21.6335f, 19.3918f)
                        curveTo(21.75f, 18.5248f, 21.75f, 17.4225f, 21.75f, 16.0549f)
                        verticalLineTo(10.9451f)
                        curveTo(21.75f, 9.5775f, 21.75f, 8.4752f, 21.6335f, 7.6083f)
                        curveTo(21.5125f, 6.7081f, 21.2536f, 5.9503f, 20.6517f, 5.3484f)
                        curveTo(20.1208f, 4.8175f, 19.4687f, 4.5535f, 18.7047f, 4.4155f)
                        curveTo(18.424f, 2.6221f, 16.8722f, 1.25f, 15f, 1.25f)
                        close()
                        moveTo(17.1293f, 4.2712f)
                        curveTo(16.8265f, 3.3862f, 15.9876f, 2.75f, 15f, 2.75f)
                        horizontalLineTo(11f)
                        curveTo(9.0932f, 2.75f, 7.7385f, 2.7516f, 6.7109f, 2.8898f)
                        curveTo(5.7048f, 3.025f, 5.1251f, 3.2787f, 4.7019f, 3.7019f)
                        curveTo(4.2787f, 4.1251f, 4.025f, 4.7048f, 3.8898f, 5.7109f)
                        curveTo(3.7516f, 6.7385f, 3.75f, 8.0932f, 3.75f, 10f)
                        verticalLineTo(16f)
                        curveTo(3.75f, 16.9876f, 4.3862f, 17.8265f, 5.2712f, 18.1293f)
                        curveTo(5.25f, 17.5194f, 5.25f, 16.8297f, 5.25f, 16.0549f)
                        verticalLineTo(10.9451f)
                        curveTo(5.25f, 9.5775f, 5.25f, 8.4752f, 5.3665f, 7.6083f)
                        curveTo(5.4875f, 6.7081f, 5.7464f, 5.9503f, 6.3484f, 5.3484f)
                        curveTo(6.9503f, 4.7464f, 7.7081f, 4.4875f, 8.6083f, 4.3665f)
                        curveTo(9.4752f, 4.25f, 10.5775f, 4.25f, 11.9451f, 4.25f)
                        horizontalLineTo(15.0549f)
                        curveTo(15.8297f, 4.25f, 16.5194f, 4.25f, 17.1293f, 4.2712f)
                        close()
                        moveTo(7.409f, 6.409f)
                        curveTo(7.6858f, 6.1323f, 8.0744f, 5.9518f, 8.8081f, 5.8532f)
                        curveTo(9.5635f, 5.7516f, 10.5646f, 5.75f, 12f, 5.75f)
                        horizontalLineTo(15f)
                        curveTo(16.4354f, 5.75f, 17.4365f, 5.7516f, 18.1919f, 5.8532f)
                        curveTo(18.9257f, 5.9518f, 19.3142f, 6.1323f, 19.591f, 6.409f)
                        curveTo(19.8678f, 6.6858f, 20.0482f, 7.0744f, 20.1469f, 7.8081f)
                        curveTo(20.2484f, 8.5635f, 20.25f, 9.5646f, 20.25f, 11f)
                        verticalLineTo(16f)
                        curveTo(20.25f, 17.4354f, 20.2484f, 18.4365f, 20.1469f, 19.1919f)
                        curveTo(20.0482f, 19.9257f, 19.8678f, 20.3142f, 19.591f, 20.591f)
                        curveTo(19.3142f, 20.8678f, 18.9257f, 21.0482f, 18.1919f, 21.1469f)
                        curveTo(17.4365f, 21.2484f, 16.4354f, 21.25f, 15f, 21.25f)
                        horizontalLineTo(12f)
                        curveTo(10.5646f, 21.25f, 9.5635f, 21.2484f, 8.8081f, 21.1469f)
                        curveTo(8.0744f, 21.0482f, 7.6858f, 20.8678f, 7.409f, 20.591f)
                        curveTo(7.1323f, 20.3142f, 6.9518f, 19.9257f, 6.8532f, 19.1919f)
                        curveTo(6.7516f, 18.4365f, 6.75f, 17.4354f, 6.75f, 16f)
                        verticalLineTo(11f)
                        curveTo(6.75f, 9.5646f, 6.7516f, 8.5635f, 6.8532f, 7.8081f)
                        curveTo(6.9518f, 7.0744f, 7.1323f, 6.6858f, 7.409f, 6.409f)
                        close()
                    }
                }
                .build()
        return _copy!!
    }

private var _copy: ImageVector? = null