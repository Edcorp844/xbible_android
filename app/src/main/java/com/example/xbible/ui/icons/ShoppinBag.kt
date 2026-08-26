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
internal val ShoppingBag: ImageVector
    get() {
        if (_shoppingBag != null) {
            return _shoppingBag!!
        }
        _shoppingBag =
            ImageVector.Builder(
                name = "ShoppingBag",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 25f,
                viewportHeight = 25f,
            )
                .apply {
                    // Path 1: Bag Body
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        pathFillType = PathFillType.EvenOdd,
                    ) {
                        moveTo(5.5006f, 16.1407f)
                        curveTo(5.4627f, 18.2335f, 7.1278f, 19.9613f, 9.2206f, 20.0007f)
                        horizontalLineTo(15.7806f)
                        curveTo(17.8735f, 19.9613f, 19.5385f, 18.2335f, 19.5006f, 16.1407f)
                        lineTo(19.0636f, 11.4527f)
                        curveTo(18.9527f, 9.6853f, 17.716f, 8.1905f, 16.0006f, 7.7507f)
                        curveTo(15.6432f, 7.6467f, 15.2729f, 7.5935f, 14.9006f, 7.5927f)
                        horizontalLineTo(10.1006f)
                        curveTo(9.7284f, 7.5935f, 9.3581f, 7.6467f, 9.0006f, 7.7507f)
                        curveTo(7.2864f, 8.1907f, 6.0503f, 9.6843f, 5.9386f, 11.4507f)
                        lineTo(5.5006f, 16.1407f)
                        close()
                    }
                    // Path 2: Handle Arc
                    path(
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(16.0006f, 9.3886f)
                        verticalLineTo(7.3676f)
                        curveTo(15.9629f, 5.4718f, 14.3965f, 3.9649f, 12.5006f, 4.0006f)
                        curveTo(10.6047f, 3.9649f, 9.0384f, 5.4718f, 9.0006f, 7.3676f)
                        verticalLineTo(9.3876f)
                    }
                }
                .build()
        return _shoppingBag!!
    }

private var _shoppingBag: ImageVector? = null