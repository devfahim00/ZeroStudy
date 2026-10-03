package com.devfahim00.zerostudy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Short "level up" celebration: a card that pops in with a ring of sparks, then fades out by itself. */
@Composable
fun LevelUpOverlay(level: Int) {
    val p = pal()
    val scale = remember(level) { Animatable(0.5f) }
    val fade = remember(level) { Animatable(0f) }
    val burst = remember(level) { Animatable(0f) }

    LaunchedEffect(level) {
        launch { fade.animateTo(1f, tween(220)) }
        launch { burst.animateTo(1f, tween(900)) }
        launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(2400)
        fade.animateTo(0f, tween(260))
        Model.dismissLevelUp()
    }

    val sparkColors = listOf(p.a, p.b, p.ok, p.warn)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * fade.value))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                Model.dismissLevelUp()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(320.dp).graphicsLayer { alpha = fade.value }) {
            val sparks = 16
            for (i in 0 until sparks) {
                val ang = i * (2.0 * Math.PI / sparks)
                val r = size.minDimension / 2f * (0.35f + 0.65f * burst.value)
                drawCircle(
                    color = sparkColors[i % sparkColors.size],
                    radius = 5.dp.toPx() * (1f - 0.6f * burst.value),
                    center = Offset(center.x + cos(ang).toFloat() * r, center.y + sin(ang).toFloat() * r),
                    alpha = 1f - burst.value
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    alpha = fade.value
                }
                .background(p.card, RoundedCornerShape(28.dp))
                .border(1.dp, p.a, RoundedCornerShape(28.dp))
                .padding(horizontal = 40.dp, vertical = 30.dp)
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = p.warn, modifier = Modifier.size(56.dp))
            Text(
                "LEVEL UP",
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 3.sp),
                color = p.a,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text(
                "Level $level",
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 40.sp),
                color = p.ink,
                modifier = Modifier.padding(top = 4.dp)
            )
            Mut("Keep going. Every session counts.", size = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
