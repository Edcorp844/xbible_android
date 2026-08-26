package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val Tabs: ImageVector
    get() {
        if (_tabs != null) {
            return _tabs!!
        }
        _tabs =
            ImageVector.Builder(
                name = "Tabs",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color(0xFF212121)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(11.25f, 7.9987f)
                        horizontalLineTo(18.75f)
                        lineTo(18.9896f, 8.0074f)
                        curveTo(20.6119f, 8.1257f, 21.9037f, 9.4347f, 21.9949f, 11.0643f)
                        lineTo(22f, 11.2487f)
                        verticalLineTo(18.7487f)
                        curveTo(22f, 20.4818f, 20.6435f, 21.898f, 18.9344f, 21.9936f)
                        lineTo(18.75f, 21.9987f)
                        horizontalLineTo(11.25f)
                        curveTo(9.517f, 21.9987f, 8.1008f, 20.6423f, 8.0051f, 18.9332f)
                        lineTo(8f, 18.7487f)
                        verticalLineTo(11.2487f)
                        curveTo(8f, 9.5157f, 9.3565f, 8.0995f, 11.0656f, 8.0039f)
                        lineTo(11.25f, 7.9987f)
                        close()
                        moveTo(9.5f, 18.7487f)
                        lineTo(9.5046f, 18.876f)
                        lineTo(9.5229f, 19.0326f)
                        curveTo(9.651f, 19.8179f, 10.3018f, 20.4276f, 11.1065f, 20.4929f)
                        lineTo(11.25f, 20.4987f)
                        horizontalLineTo(18.75f)
                        curveTo(19.6682f, 20.4987f, 20.4212f, 19.7916f, 20.4942f, 18.8923f)
                        lineTo(20.5f, 18.7487f)
                        verticalLineTo(11.2487f)
                        curveTo(20.5f, 10.2822f, 19.7165f, 9.4987f, 18.75f, 9.4987f)
                        horizontalLineTo(11.25f)
                        curveTo(10.3318f, 9.4987f, 9.5788f, 10.2059f, 9.5058f, 11.1052f)
                        lineTo(9.5f, 11.2487f)
                        verticalLineTo(18.7487f)
                        close()
                        moveTo(15.75f, 5f)
                        curveTo(15.8306f, 5f, 15.9105f, 5.0029f, 15.9896f, 5.0087f)
                        curveTo(17.2369f, 5.0995f, 18.2891f, 5.8945f, 18.7501f, 6.998f)
                        lineTo(16.9723f, 6.9976f)
                        curveTo(16.6889f, 6.721f, 16.312f, 6.5398f, 15.8935f, 6.5058f)
                        lineTo(15.75f, 6.5f)
                        horizontalLineTo(8.25f)
                        curveTo(7.3318f, 6.5f, 6.5788f, 7.2071f, 6.5058f, 8.1065f)
                        lineTo(6.5f, 8.25f)
                        verticalLineTo(15.75f)
                        lineTo(6.5108f, 15.9458f)
                        curveTo(6.5552f, 16.3447f, 6.7337f, 16.7031f, 7.0002f, 16.9749f)
                        lineTo(7.0001f, 18.7509f)
                        curveTo(5.8793f, 18.2836f, 5.0763f, 17.2067f, 5.0051f, 15.9344f)
                        lineTo(5f, 15.75f)
                        verticalLineTo(8.25f)
                        curveTo(5f, 6.517f, 6.3565f, 5.1008f, 8.0656f, 5.0051f)
                        lineTo(8.25f, 5f)
                        horizontalLineTo(15.75f)
                        close()
                        moveTo(12.75f, 2f)
                        curveTo(14.0498f, 2f, 15.1713f, 2.763f, 15.6912f, 3.8655f)
                        lineTo(15.75f, 4f)
                        lineTo(13.9733f, 3.9986f)
                        curveTo(13.6898f, 3.7214f, 13.3125f, 3.5398f, 12.8935f, 3.5058f)
                        lineTo(12.75f, 3.5f)
                        horizontalLineTo(5.25f)
                        curveTo(4.3318f, 3.5f, 3.5788f, 4.2071f, 3.5058f, 5.1065f)
                        lineTo(3.5f, 5.25f)
                        verticalLineTo(12.75f)
                        curveTo(3.5f, 13.2269f, 3.6908f, 13.6593f, 4.0002f, 13.9749f)
                        lineTo(4.0001f, 15.7509f)
                        curveTo(2.8793f, 15.2836f, 2.0763f, 14.2067f, 2.0051f, 12.9344f)
                        lineTo(2f, 12.75f)
                        verticalLineTo(5.25f)
                        curveTo(2f, 3.517f, 3.3565f, 2.1008f, 5.0656f, 2.0051f)
                        lineTo(5.25f, 2f)
                        horizontalLineTo(12.75f)
                        close()
                    }
                }
                .build()
        return _tabs!!
    }

private var _tabs: ImageVector? = null