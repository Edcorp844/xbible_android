package com.example.xbible.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")

internal val Notes: ImageVector
    get() {
        if (_notes != null) {
            return _notes!!
        }
        _notes =
            ImageVector.Builder(
                name = "Notes",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
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
                        moveTo(20.3116f, 12.6473f)
                        lineTo(20.8293f, 10.7154f)
                        curveTo(21.4335f, 8.4603f, 21.7356f, 7.3328f, 21.5081f, 6.357f)
                        curveTo(21.3285f, 5.5866f, 20.9244f, 4.8867f, 20.347f, 4.3459f)
                        curveTo(19.6157f, 3.661f, 18.4881f, 3.3588f, 16.2331f, 2.7546f)
                        curveTo(13.978f, 2.1503f, 12.8504f, 1.8482f, 11.8747f, 2.0757f)
                        curveTo(11.1042f, 2.2554f, 10.4043f, 2.6595f, 9.8635f, 3.2369f)
                        curveTo(9.2771f, 3.863f, 8.9713f, 4.7796f, 8.5162f, 6.4456f)
                        curveTo(8.4398f, 6.7254f, 8.3591f, 7.0263f, 8.2723f, 7.3506f)
                        lineTo(8.2722f, 7.3508f)
                        lineTo(7.7546f, 9.2826f)
                        curveTo(7.1503f, 11.5377f, 6.8482f, 12.6652f, 7.0757f, 13.641f)
                        curveTo(7.2554f, 14.4115f, 7.6595f, 15.1114f, 8.2369f, 15.6522f)
                        curveTo(8.9681f, 16.3371f, 10.0957f, 16.6392f, 12.3508f, 17.2435f)
                        lineTo(12.3508f, 17.2435f)
                        curveTo(14.3834f, 17.7881f, 15.4999f, 18.0873f, 16.415f, 17.9744f)
                        curveTo(16.5152f, 17.9621f, 16.6129f, 17.9448f, 16.7092f, 17.9223f)
                        curveTo(17.4796f, 17.7427f, 18.1795f, 17.3386f, 18.7203f, 16.7612f)
                        curveTo(19.4052f, 16.0299f, 19.7074f, 14.9024f, 20.3116f, 12.6473f)
                        close()
                    }
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(16.415f, 17.9741f)
                        curveTo(16.2065f, 18.6126f, 15.8399f, 19.1902f, 15.347f, 19.6519f)
                        curveTo(14.6157f, 20.3368f, 13.4881f, 20.6389f, 11.2331f, 21.2432f)
                        curveTo(8.978f, 21.8474f, 7.8504f, 22.1495f, 6.8747f, 21.922f)
                        curveTo(6.1042f, 21.7424f, 5.4043f, 21.3383f, 4.8635f, 20.7609f)
                        curveTo(4.1786f, 20.0296f, 3.8765f, 18.9021f, 3.2722f, 16.647f)
                        lineTo(2.7546f, 14.7151f)
                        curveTo(2.1503f, 12.46f, 1.8482f, 11.3325f, 2.0757f, 10.3567f)
                        curveTo(2.2554f, 9.5863f, 2.6595f, 8.8864f, 3.2369f, 8.3456f)
                        curveTo(3.9681f, 7.6607f, 5.0957f, 7.3585f, 7.3508f, 6.7543f)
                        curveTo(7.7774f, 6.64f, 8.1637f, 6.5365f, 8.5162f, 6.4453f)
                    }
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(11.7769f, 10f)
                        lineTo(16.6065f, 11.2941f)
                    }
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Miter,
                    ) {
                        moveTo(11f, 12.8975f)
                        lineTo(13.8978f, 13.6739f)
                    }
                }
                .build()
        return _notes!!
    }

private var _notes: ImageVector? = null