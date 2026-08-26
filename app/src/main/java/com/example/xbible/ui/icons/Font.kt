package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val FontSize: ImageVector
    get() {
        if (_fontSize != null) {
            return _fontSize!!
        }
        _fontSize =
            ImageVector.Builder(
                name = "FontSize",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color(0xFF1C274C)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Normalized from 1200x1200 viewport to 24x24 viewport (scaled by 1/50)

                        // Inner bowl of lowercase 'a'
                        moveTo(20.8256f, 15.8655f)
                        curveTo(20.5867f, 15.473f, 20.2884f, 15.2213f, 19.9304f, 15.1104f)
                        curveTo(19.1871f, 14.8725f, 18.357f, 14.8887f, 17.8564f, 15.428f)
                        curveTo(17.2968f, 16.1692f, 17.2828f, 17.2801f, 17.848f, 17.924f)
                        curveTo(18.7229f, 18.7499f, 20.2681f, 18.3417f, 20.8176f, 17.5312f)
                        verticalLineTo(15.8655f)
                        close()

                        // Outer body of lowercase 'a'
                        moveTo(22.7968f, 9.2352f)
                        curveTo(23.5989f, 9.9691f, 24.0f, 11.0101f, 24.0f, 12.3584f)
                        verticalLineTo(20.1407f)
                        curveTo(23.9869f, 20.3557f, 23.8105f, 20.5219f, 23.616f, 20.5247f)
                        horizontalLineTo(21.2097f)
                        curveTo(20.9946f, 20.5117f, 20.8284f, 20.3351f, 20.8256f, 20.1407f)
                        verticalLineTo(19.9103f)
                        curveTo(20.0223f, 20.5199f, 19.0929f, 20.8008f, 18.1633f, 20.8063f)
                        curveTo(17.1085f, 20.7754f, 16.1918f, 20.4022f, 15.4796f, 19.7439f)
                        curveTo(14.5976f, 18.8613f, 14.2578f, 17.7761f, 14.2508f, 16.6585f)
                        curveTo(14.2855f, 15.4364f, 14.6384f, 14.3364f, 15.4932f, 13.548f)
                        curveTo(16.2809f, 12.8817f, 17.2126f, 12.5686f, 18.1678f, 12.5624f)
                        curveTo(19.2259f, 12.5624f, 20.1134f, 12.844f, 20.8256f, 13.4088f)
                        verticalLineTo(12.6664f)
                        curveTo(20.8256f, 11.4888f, 20.2112f, 10.8976f, 18.9828f, 10.8976f)
                        curveTo(18.0271f, 10.8976f, 17.1567f, 11.2987f, 16.3716f, 12.1018f)
                        curveTo(16.1632f, 12.2863f, 15.8693f, 12.2139f, 15.7572f, 11.999f)
                        lineTo(14.81f, 10.3606f)
                        curveTo(14.7247f, 10.19f, 14.7503f, 10.0363f, 14.8868f, 9.8998f)
                        curveTo(15.4671f, 9.3195f, 16.1412f, 8.8798f, 16.9084f, 8.5814f)
                        curveTo(18.8121f, 7.9222f, 21.3523f, 7.9586f, 22.7968f, 9.2352f)
                        close()

                        // Counter hole of uppercase 'A'
                        moveTo(7.0531f, 7.9553f)
                        lineTo(5.3635f, 13.9968f)
                        horizontalLineTo(8.7682f)
                        lineTo(7.0531f, 7.9553f)
                        close()

                        // Body of uppercase 'A'
                        moveTo(14.0929f, 19.8591f)
                        curveTo(14.127f, 19.9615f, 14.1442f, 20.0298f, 14.1442f, 20.064f)
                        curveTo(14.137f, 20.2917f, 13.9622f, 20.4454f, 13.7601f, 20.448f)
                        horizontalLineTo(13.7345f)
                        horizontalLineTo(10.7394f)
                        curveTo(10.5517f, 20.448f, 10.4322f, 20.3542f, 10.381f, 20.1664f)
                        lineTo(9.5362f, 17.1968f)
                        horizontalLineTo(4.5955f)
                        lineTo(3.7507f, 20.1664f)
                        curveTo(3.6995f, 20.3542f, 3.5715f, 20.448f, 3.3672f, 20.448f)
                        horizontalLineTo(0.3972f)
                        curveTo(0.078f, 20.4215f, -0.0427f, 20.2255f, 0.0132f, 19.9617f)
                        lineTo(5.0307f, 3.4755f)
                        curveTo(5.099f, 3.2878f, 5.227f, 3.1939f, 5.4147f, 3.1939f)
                        horizontalLineTo(8.7171f)
                        curveTo(8.9048f, 3.1939f, 9.0328f, 3.2878f, 9.1012f, 3.4755f)
                        lineTo(14.0929f, 19.8591f)
                        close()
                    }
                }
                .build()
        return _fontSize!!
    }

private var _fontSize: ImageVector? = null