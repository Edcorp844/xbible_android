package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val Compare: ImageVector
    get() {
        if (_compare != null) {
            return _compare!!
        }
        _compare =
            ImageVector.Builder(
                name = "Compare",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    // Top Layer
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                    ) {
                        moveTo(4.9788f, 9.6851f)
                        curveTo(2.9929f, 8.8907f, 2f, 8.4936f, 2f, 8f)
                        curveTo(2f, 7.5065f, 2.9929f, 7.1093f, 4.9788f, 6.3149f)
                        lineTo(7.7873f, 5.1915f)
                        curveTo(9.7732f, 4.3972f, 10.7661f, 4f, 12f, 4f)
                        curveTo(13.2339f, 4f, 14.2268f, 4.3972f, 16.2127f, 5.1915f)
                        lineTo(19.0212f, 6.3149f)
                        curveTo(21.0071f, 7.1093f, 22f, 7.5065f, 22f, 8f)
                        curveTo(22f, 8.4936f, 21.0071f, 8.8907f, 19.0212f, 9.6851f)
                        lineTo(16.2127f, 10.8085f)
                        curveTo(14.2268f, 11.6028f, 13.2339f, 12f, 12f, 12f)
                        curveTo(10.7661f, 12f, 9.7732f, 11.6028f, 7.7873f, 10.8085f)
                        lineTo(4.9788f, 9.6851f)
                        close()
                    }

                    // Middle Layer
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                    ) {
                        moveTo(5.7661f, 10f)
                        lineTo(4.9788f, 10.3149f)
                        curveTo(2.9929f, 11.1093f, 2f, 11.5065f, 2f, 12f)
                        curveTo(2f, 12.4935f, 2.9929f, 12.8907f, 4.9788f, 13.6851f)
                        lineTo(7.7873f, 14.8085f)
                        curveTo(9.7732f, 15.6028f, 10.7661f, 16f, 12f, 16f)
                        curveTo(13.2339f, 16f, 14.2268f, 15.6028f, 16.2127f, 14.8085f)
                        lineTo(19.0212f, 13.6851f)
                        curveTo(21.0071f, 12.8907f, 22f, 12.4935f, 22f, 12f)
                        curveTo(22f, 11.5065f, 21.0071f, 11.1093f, 19.0212f, 10.3149f)
                        lineTo(18.2339f, 10f)
                    }

                    // Bottom Layer
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                    ) {
                        moveTo(5.7661f, 14f)
                        lineTo(4.9788f, 14.3149f)
                        curveTo(2.9929f, 15.1093f, 2f, 15.5065f, 2f, 16f)
                        curveTo(2f, 16.4935f, 2.9929f, 16.8907f, 4.9788f, 17.6851f)
                        lineTo(7.7873f, 18.8085f)
                        curveTo(9.7732f, 19.6028f, 10.7661f, 20f, 12f, 20f)
                        curveTo(13.2339f, 20f, 14.2268f, 19.6028f, 16.2127f, 18.8085f)
                        lineTo(19.0212f, 17.6851f)
                        curveTo(21.0071f, 16.8907f, 22f, 16.4935f, 22f, 16f)
                        curveTo(22f, 15.5065f, 21.0071f, 15.1093f, 19.0212f, 14.3149f)
                        lineTo(18.2339f, 14f)
                    }
                }
                .build()
        return _compare!!
    }

private var _compare: ImageVector? = null