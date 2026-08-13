package com.example.xbible.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.xbible_engine.SwordModule

@Composable
fun BookView(
    module: SwordModule,
    modifier: Modifier = Modifier
) {
    val hue = module.signatureColor.hue.toFloat()
    val saturation = module.signatureColor.saturation.toFloat()
    val brightness = module.signatureColor.brightness.toFloat()

    // Simplified color conversion for Android (HSV to Color)
    // Note: HSV in Compose is h in [0, 360], s in [0, 1], v in [0, 1]
    // Rust side might be [0, 1] for all. 
    val baseColor = Color.hsv(hue * 360f, saturation, brightness)

    Box(
        modifier = modifier
            .width(140.dp)
            .height(200.dp)
            .shadow(8.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp))
            .background(baseColor)
    ) {
        // Spine Crease / 3D effect
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(20.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.25f to Color.White.copy(alpha = 0.15f),
                        1f to Color.Transparent
                    )
                )
        )

        // Spine Border Highlight
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .padding(start = 1.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Cover Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = module.description,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 8
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Version ${module.version}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
