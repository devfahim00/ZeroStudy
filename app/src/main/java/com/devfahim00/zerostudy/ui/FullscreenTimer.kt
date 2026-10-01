package com.devfahim00.zerostudy.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.p2

@Composable
fun FullscreenTimer() {
    BackHandler { Model.fullscreen = false }

    val ctx = LocalContext.current
    DisposableEffect(Unit) {
        val window = (ctx as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(56.dp)
        ) {
            FlipClock()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FsCircleBtn(
                    icon = if (Model.S.tm.run) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    desc = if (Model.S.tm.run) "Pause" else "Resume"
                ) { Model.toggle() }
                FsCircleBtn(icon = Icons.Rounded.Close, desc = "Exit") { Model.fullscreen = false }
            }
        }
    }
}

@Composable
private fun FsCircleBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { alpha = if (pressed) 1f else 0.35f }
            .background(Color(0xFF000000), CircleShape)
            .border(1.dp, Color(0xFF1B1E29), CircleShape)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = Color(0xFF99A0AA), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun FlipClock() {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fsDp = minOf(maxWidth * 0.155f, maxHeight * 0.28f)
        val sec = Model.dispSec()
        val digits = p2((sec / 3600).toLong()) + p2((sec % 3600 / 60).toLong()) + p2((sec % 60).toLong())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(fsDp * 0.06f)
        ) {
            digits.forEachIndexed { i, ch ->
                if (i == 2 || i == 4) {
                    Text(
                        ":",
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = with(density) { (fsDp * 0.5f).toSp() }),
                        color = Color(0xFF2A2D38)
                    )
                }
                FlipDigit(ch, fsDp)
            }
        }
    }
}

@Composable
private fun FlipDigit(newDigit: Char, fsDp: androidx.compose.ui.unit.Dp) {
    var old by remember { mutableStateOf(newDigit) }
    val anim = remember { Animatable(1f) }
    val density = LocalDensity.current
    val fontSize = with(density) { fsDp.toSp() }

    LaunchedEffect(newDigit) {
        if (newDigit != old) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(600, easing = LinearEasing))
            old = newDigit
        }
    }
    val t = anim.value
    val w = fsDp * 0.78f
    val h = fsDp * 1.3f

    Box(Modifier.size(w, h)) {
        // static new top half
        Half(newDigit, isTop = true, rotX = 0f, fontSize = fontSize, height = h)
        // static bottom half (old until the flap covers it)
        Half(if (t >= 1f) newDigit else old, isTop = false, rotX = 0f, fontSize = fontSize, height = h)
        // flap A: old top half rotating out during the first half of the animation
        if (t < 0.5f) {
            Half(old, isTop = true, rotX = -90f * (t / 0.5f), fontSize = fontSize, height = h)
        }
        // flap B: new bottom half rotating in during the second half
        val secondHalf = ((t - 0.5f) / 0.5f).coerceIn(0f, 1f)
        Half(newDigit, isTop = false, rotX = 90f * (1f - secondHalf), fontSize = fontSize, height = h)
        // middle divider
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFF000000))
        )
    }
}

@Composable
private fun BoxScope.Half(digit: Char, isTop: Boolean, rotX: Float, fontSize: TextUnit, height: androidx.compose.ui.unit.Dp) {
    val topBg = Color(0xFF0F1014)
    val botBg = Color(0xFF0B0C10)
    Box(
        Modifier
            .align(if (isTop) Alignment.TopCenter else Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .clipToBounds()
            .graphicsLayer {
                rotationX = rotX
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(0.5f, if (isTop) 1f else 0f)
            }
            .background(if (isTop) topBg else botBg)
    ) {
        Box(Modifier.fillMaxWidth().height(height)) {
            Text(
                digit.toString(),
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = fontSize),
                color = Color(0xFFE8EBF2),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
