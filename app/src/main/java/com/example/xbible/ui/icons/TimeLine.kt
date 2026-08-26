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
internal val TimeLine: ImageVector
    get() {
        if (_timeLine != null) {
            return _timeLine!!
        }
        _timeLine =
            ImageVector.Builder(
                name = "TimeLine",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 512f,
                viewportHeight = 512f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Central vertical axis spine & node rings
                        moveTo(256f, 208.49f)
                        curveToRelative(-11.502f, 0f, -20.86f, -9.357f, -20.86f, -20.86f)
                        curveToRelative(0f, -11.507f, 9.358f, -20.87f, 20.86f, -20.87f)
                        curveToRelative(11.508f, 0f, 20.87f, 9.363f, 20.87f, 20.87f)
                        curveToRelative(0f, 11.503f, -9.362f, 20.86f, -20.87f, 20.86f)
                        moveToRelative(0f, 136.76f)
                        curveToRelative(-11.502f, 0f, -20.86f, -9.363f, -20.86f, -20.87f)
                        curveToRelative(0f, -11.507f, 9.358f, -20.87f, 20.86f, -20.87f)
                        curveToRelative(11.508f, 0f, 20.87f, 9.363f, 20.87f, 20.87f)
                        curveToRelative(0f, 11.507f, -9.362f, 20.87f, -20.87f, 20.87f)
                        moveToRelative(0f, 136.75f)
                        curveToRelative(-11.502f, 0f, -20.86f, -9.358f, -20.86f, -20.86f)
                        curveToRelative(0f, -11.507f, 9.358f, -20.871f, 20.86f, -20.871f)
                        curveToRelative(11.508f, 0f, 20.87f, 9.364f, 20.87f, 20.871f)
                        curveToRelative(0f, 11.502f, -9.362f, 20.86f, -20.87f, 20.86f)
                        moveToRelative(0f, -452f)
                        curveToRelative(11.508f, 0f, 20.87f, 9.362f, 20.87f, 20.87f)
                        curveToRelative(0f, 11.508f, -9.362f, 20.87f, -20.87f, 20.87f)
                        curveToRelative(-11.502f, 0f, -20.86f, -9.362f, -20.86f, -20.87f)
                        curveToRelative(0f, -11.508f, 9.358f, -20.87f, 20.86f, -20.87f)
                        moveToRelative(50.87f, 20.87f)
                        curveToRelative(0f, -28.05f, -22.82f, -50.87f, -50.87f, -50.87f)
                        curveToRelative(-28.044f, 0f, -50.86f, 22.82f, -50.86f, 50.87f)
                        curveToRelative(0f, 22.829f, 15.114f, 42.192f, 35.86f, 48.61f)
                        verticalLineToRelative(39.54f)
                        curveToRelative(-20.746f, 6.417f, -35.86f, 25.781f, -35.86f, 48.61f)
                        curveToRelative(0f, 22.825f, 15.114f, 42.184f, 35.86f, 48.6f)
                        verticalLineToRelative(39.54f)
                        curveToRelative(-20.746f, 6.417f, -35.86f, 25.781f, -35.86f, 48.61f)
                        curveToRelative(0f, 22.829f, 15.114f, 42.192f, 35.86f, 48.61f)
                        verticalLineToRelative(39.54f)
                        curveToRelative(-20.746f, 6.417f, -35.86f, 25.78f, -35.86f, 48.61f)
                        curveToRelative(0f, 28.044f, 22.816f, 50.86f, 50.86f, 50.86f)
                        curveToRelative(28.05f, 0f, 50.87f, -22.816f, 50.87f, -50.86f)
                        curveToRelative(0f, -22.831f, -15.119f, -42.195f, -35.87f, -48.611f)
                        verticalLineToRelative(-39.539f)
                        curveToRelative(20.751f, -6.416f, 35.87f, -25.779f, 35.87f, -48.61f)
                        curveToRelative(0f, -22.831f, -15.119f, -42.195f, -35.87f, -48.611f)
                        verticalLineToRelative(-39.538f)
                        curveToRelative(20.751f, -6.415f, 35.87f, -25.775f, 35.87f, -48.601f)
                        curveToRelative(0f, -22.831f, -15.119f, -42.195f, -35.87f, -48.611f)
                        verticalLineToRelative(-39.539f)
                        curveToRelative(20.751f, -6.416f, 35.87f, -25.779f, 35.87f, -48.61f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Upper-left pill card
                        moveTo(140.22f, 61.476f)
                        curveToRelative(-2.836f, 2.836f, -6.605f, 4.397f, -10.614f, 4.397f)
                        horizontalLineToRelative(-79.18f)
                        curveToRelative(-4.008f, 0f, -7.77f, -1.557f, -10.613f, -4.405f)
                        curveToRelative(-2.829f, -2.824f, -4.387f, -6.588f, -4.387f, -10.595f)
                        curveToRelative(0f, -4.006f, 1.558f, -7.77f, 4.405f, -10.612f)
                        curveToRelative(2.825f, -2.83f, 6.587f, -4.388f, 10.595f, -4.388f)
                        horizontalLineToRelative(79.18f)
                        curveToRelative(4.009f, 0f, 7.778f, 1.562f, 10.613f, 4.396f)
                        curveToRelative(2.788f, 2.789f, 4.387f, 6.654f, 4.387f, 10.604f)
                        curveToRelative(0f, 3.951f, -1.599f, 7.816f, -4.386f, 10.603f)
                        moveToRelative(-10.614f, -55.603f)
                        horizontalLineToRelative(-79.18f)
                        curveToRelative(-12.031f, 0f, -23.333f, 4.685f, -31.808f, 13.175f)
                        curveToRelative(-8.507f, 8.492f, -13.192f, 19.795f, -13.192f, 31.825f)
                        curveToRelative(0f, 12.031f, 4.685f, 23.333f, 13.174f, 31.808f)
                        curveToRelative(8.493f, 8.508f, 19.795f, 13.192f, 31.826f, 13.192f)
                        horizontalLineToRelative(79.18f)
                        curveToRelative(12.021f, 0f, 23.324f, -4.681f, 31.827f, -13.184f)
                        curveToRelative(8.495f, -8.494f, 13.173f, -19.794f, 13.173f, -31.816f)
                        curveToRelative(0f, -12.021f, -4.678f, -23.321f, -13.174f, -31.817f)
                        curveToRelative(-8.502f, -8.501f, -19.805f, -13.183f, -31.826f, -13.183f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Lower-left pill card
                        moveTo(140.22f, 334.988f)
                        curveToRelative(-2.835f, 2.835f, -6.605f, 4.396f, -10.614f, 4.396f)
                        horizontalLineToRelative(-79.18f)
                        curveToRelative(-4.008f, 0f, -7.77f, -1.557f, -10.613f, -4.405f)
                        curveToRelative(-2.829f, -2.824f, -4.387f, -6.587f, -4.387f, -10.595f)
                        curveToRelative(0f, -4.006f, 1.558f, -7.77f, 4.405f, -10.612f)
                        curveToRelative(2.825f, -2.83f, 6.587f, -4.388f, 10.595f, -4.388f)
                        horizontalLineToRelative(79.18f)
                        curveToRelative(4.009f, 0f, 7.778f, 1.562f, 10.613f, 4.396f)
                        curveToRelative(2.788f, 2.789f, 4.387f, 6.654f, 4.387f, 10.604f)
                        curveToRelative(0f, 4.009f, -1.558f, 7.776f, -4.386f, 10.604f)
                        moveToRelative(-10.614f, -55.604f)
                        horizontalLineToRelative(-79.18f)
                        curveToRelative(-12.031f, 0f, -23.333f, 4.685f, -31.808f, 13.175f)
                        curveToRelative(-8.507f, 8.492f, -13.192f, 19.795f, -13.192f, 31.825f)
                        curveToRelative(0f, 12.031f, 4.685f, 23.333f, 13.174f, 31.808f)
                        curveToRelative(8.492f, 8.508f, 19.795f, 13.192f, 31.826f, 13.192f)
                        horizontalLineToRelative(79.18f)
                        curveToRelative(12.022f, 0f, 23.324f, -4.681f, 31.827f, -13.183f)
                        curveToRelative(8.495f, -8.495f, 13.173f, -19.794f, 13.173f, -31.817f)
                        curveToRelative(0f, -12.021f, -4.678f, -23.321f, -13.174f, -31.817f)
                        curveToRelative(-8.502f, -8.501f, -19.805f, -13.183f, -31.826f, -13.183f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Upper-right pill card
                        moveTo(371.77f, 177.022f)
                        curveToRelative(2.799f, -2.793f, 6.667f, -4.396f, 10.612f, -4.396f)
                        horizontalLineToRelative(79.191f)
                        curveToRelative(4.009f, 0f, 7.774f, 1.558f, 10.603f, 4.387f)
                        curveToRelative(2.835f, 2.835f, 4.397f, 6.605f, 4.397f, 10.613f)
                        curveToRelative(0f, 4.003f, -1.562f, 7.769f, -4.397f, 10.604f)
                        curveToRelative(-2.794f, 2.794f, -6.659f, 4.396f, -10.603f, 4.396f)
                        horizontalLineToRelative(-79.191f)
                        curveToRelative(-3.939f, 0f, -7.807f, -1.606f, -10.593f, -4.386f)
                        curveToRelative(-2.8f, -2.806f, -4.407f, -6.674f, -4.407f, -10.614f)
                        curveToRelative(0f, -4.003f, 1.565f, -7.776f, 4.388f, -10.604f)
                        moveToRelative(10.612f, 55.604f)
                        horizontalLineToRelative(79.191f)
                        curveToRelative(12.015f, 0f, 23.314f, -4.681f, 31.817f, -13.183f)
                        curveToRelative(8.501f, -8.502f, 13.183f, -19.802f, 13.183f, -31.817f)
                        curveToRelative(0f, -12.022f, -4.682f, -23.325f, -13.184f, -31.826f)
                        curveToRelative(-8.495f, -8.495f, -19.794f, -13.174f, -31.816f, -13.174f)
                        horizontalLineToRelative(-79.191f)
                        curveToRelative(-12.007f, 0f, -23.303f, 4.675f, -31.826f, 13.183f)
                        curveToRelative(-8.495f, 8.511f, -13.174f, 19.811f, -13.174f, 31.817f)
                        curveToRelative(0f, 12.001f, 4.679f, 23.297f, 13.194f, 31.828f)
                        curveToRelative(8.509f, 8.494f, 19.805f, 13.172f, 31.806f, 13.172f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Lower-right pill card
                        moveTo(472.18f, 471.748f)
                        curveToRelative(-2.794f, 2.793f, -6.659f, 4.396f, -10.604f, 4.396f)
                        horizontalLineToRelative(-79.19f)
                        curveToRelative(-3.939f, 0f, -7.808f, -1.606f, -10.594f, -4.387f)
                        curveToRelative(-2.8f, -2.806f, -4.406f, -6.673f, -4.406f, -10.613f)
                        curveToRelative(0f, -4.003f, 1.565f, -7.777f, 4.387f, -10.604f)
                        curveToRelative(2.799f, -2.794f, 6.667f, -4.396f, 10.613f, -4.396f)
                        horizontalLineToRelative(79.19f)
                        curveToRelative(3.952f, 0f, 7.816f, 1.599f, 10.603f, 4.385f)
                        curveToRelative(2.836f, 2.836f, 4.397f, 6.606f, 4.397f, 10.615f)
                        curveToRelative(0f, 4.002f, -1.561f, 7.768f, -4.396f, 10.604f)
                        moveToRelative(-10.604f, -55.604f)
                        horizontalLineToRelative(-79.19f)
                        curveToRelative(-12.007f, 0f, -23.303f, 4.674f, -31.826f, 13.182f)
                        curveToRelative(-8.495f, 8.512f, -13.174f, 19.811f, -13.174f, 31.818f)
                        curveToRelative(0f, 12f, 4.679f, 23.297f, 13.192f, 31.826f)
                        curveToRelative(8.511f, 8.495f, 19.807f, 13.174f, 31.808f, 13.174f)
                        horizontalLineToRelative(79.19f)
                        curveToRelative(12.016f, 0f, 23.316f, -4.682f, 31.817f, -13.183f)
                        curveToRelative(8.502f, -8.502f, 13.183f, -19.801f, 13.183f, -31.817f)
                        curveToRelative(0f, -12.021f, -4.681f, -23.324f, -13.183f, -31.828f)
                        curveToRelative(-8.495f, -8.494f, -19.795f, -13.172f, -31.817f, -13.172f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Horizontal connector line 1 (Top Left)
                        moveTo(159.61f, 142.37f)
                        horizontalLineToRelative(-110.95f)
                        curveToRelative(-8.284f, 0f, -15f, 6.716f, -15f, 15f)
                        curveToRelative(0f, 8.284f, 6.716f, 15f, 15f, 15f)
                        horizontalLineToRelative(110.95f)
                        curveToRelative(8.284f, 0f, 15f, -6.716f, 15f, -15f)
                        curveToRelative(0f, -8.284f, -6.716f, -15f, -15f, -15f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Horizontal connector line 2 (Mid-Top Left)
                        moveTo(159.61f, 202.89f)
                        horizontalLineToRelative(-49.59f)
                        curveToRelative(-8.284f, 0f, -15f, 6.715f, -15f, 15f)
                        curveToRelative(0f, 8.283f, 6.716f, 15f, 15f, 15f)
                        horizontalLineToRelative(49.59f)
                        curveToRelative(8.284f, 0f, 15f, -6.717f, 15f, -15f)
                        curveToRelative(0f, -8.285f, -6.716f, -15f, -15f, -15f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Horizontal connector line 3 (Mid-Bottom Right)
                        moveTo(352.38f, 303.12f)
                        horizontalLineToRelative(110.96f)
                        curveToRelative(8.284f, 0f, 15f, -6.716f, 15f, -15f)
                        curveToRelative(0f, -8.284f, -6.716f, -15f, -15f, -15f)
                        horizontalLineToRelative(-110.96f)
                        curveToRelative(-8.284f, 0f, -15f, 6.716f, -15f, 15f)
                        curveToRelative(0f, 8.284f, 6.716f, 15f, 15f, 15f)
                    }
                    path(
                        fill = SolidColor(Color(0xFF000000)),
                        pathFillType = PathFillType.NonZero,
                    ) {
                        // Horizontal connector line 4 (Bottom Right)
                        moveTo(352.38f, 363.64f)
                        horizontalLineToRelative(49.601f)
                        curveToRelative(8.284f, 0f, 15f, -6.717f, 15f, -15f)
                        curveToRelative(0f, -8.285f, -6.716f, -15f, -15f, -15f)
                        horizontalLineToRelative(-49.601f)
                        curveToRelative(-8.284f, 0f, -15f, 6.715f, -15f, 15f)
                        curveToRelative(0f, 8.283f, 6.716f, 15f, 15f, 15f)
                    }
                }
                .build()
        return _timeLine!!
    }

private var _timeLine: ImageVector? = null

@Suppress("CheckReturnValue")
internal val ParallelMode: ImageVector
    get() {
        if (_parallelMode != null) {
            return _parallelMode!!
        }
        _parallelMode =
            ImageVector.Builder(
                name = "ParallelMode",
                defaultWidth = 32.dp,
                defaultHeight = 32.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    // Outer Frame / Book Border
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(10f, 3f)
                        horizontalLineTo(14f)
                        curveTo(16.8284f, 3f, 18.2426f, 3f, 19.1213f, 3.8787f)
                        curveTo(20f, 4.7574f, 20f, 6.1716f, 20f, 9f)
                        verticalLineTo(15f)
                        curveTo(20f, 17.8284f, 20f, 19.2426f, 19.1213f, 20.1213f)
                        curveTo(18.2426f, 21f, 16.8284f, 21f, 14f, 21f)
                        horizontalLineTo(10f)
                        curveTo(7.1716f, 21f, 5.7574f, 21f, 4.8787f, 20.1213f)
                        curveTo(4f, 19.2426f, 4f, 17.8284f, 4f, 15f)
                        verticalLineTo(9f)
                        curveTo(4f, 6.1716f, 4f, 4.7574f, 4.8787f, 3.8787f)
                        curveTo(5.7574f, 3f, 7.1716f, 3f, 10f, 3f)
                        close()
                    }

                    // Center Parallel Split Divider
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                    ) {
                        moveTo(12f, 3f)
                        lineTo(12f, 21f)
                    }

                    // Left Page Text Lines
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                    ) {
                        moveTo(7f, 8f)
                        horizontalLineTo(9.5f)
                        moveTo(7f, 12f)
                        horizontalLineTo(9.5f)
                        moveTo(7f, 16f)
                        horizontalLineTo(9f)
                    }

                    // Right Page Text Lines
                    path(
                        stroke = SolidColor(Color(0xFF1C274C)),
                        strokeLineWidth = 1.5f,
                        strokeLineCap = StrokeCap.Round,
                    ) {
                        moveTo(14.5f, 8f)
                        horizontalLineTo(17f)
                        moveTo(14.5f, 12f)
                        horizontalLineTo(17f)
                        moveTo(14.5f, 16f)
                        horizontalLineTo(16.5f)
                    }
                }
                .build()
        return _parallelMode!!
    }

private var _parallelMode: ImageVector? = null