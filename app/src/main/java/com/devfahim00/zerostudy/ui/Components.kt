package com.devfahim00.zerostudy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager

/* ---------- card ---------- */

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(pal().card, RoundedCornerShape(22.dp))
            .border(1.dp, pal().line, RoundedCornerShape(22.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun H2(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
        color = pal().mut,
        modifier = modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun Mut(text: String, modifier: Modifier = Modifier, size: TextUnit = 13.sp, color: Color = pal().mut) {
    Text(
        text = text,
        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = size),
        color = color,
        modifier = modifier
    )
}

/* ---------- pill / chip ---------- */

@Composable
fun Pill(icon: ImageVector, text: String) {
    val p = pal()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(p.card, RoundedCornerShape(99.dp))
            .border(1.dp, p.line, RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = p.a, modifier = Modifier.size(16.dp))
        Mut(text, size = 13.sp)
    }
}

@Composable
fun Chip(text: String, on: Boolean, color: Color = pal().a, onClick: () -> Unit) {
    val p = pal()
    val bg = if (on) lerp(p.card, color, 0.16f) else p.card
    Text(
        text = text,
        style = TextStyle(
            fontFamily = Sora,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp
        ),
        color = if (on) p.ink else p.mut,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(bg, RoundedCornerShape(99.dp))
            .border(1.dp, if (on) color else p.line, RoundedCornerShape(99.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    )
}

/* ---------- progress bar ---------- */

@Composable
fun Bar(fill: Float, height: Dp = 8.dp, color: Color? = null, modifier: Modifier = Modifier) {
    val p = pal()
    val animated by animateFloatAsState(targetValue = fill.coerceIn(0f, 1f), animationSpec = tween(400), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(p.trk, RoundedCornerShape(99.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .background(
                    color?.let { Brush.horizontalGradient(listOf(it, it)) }
                        ?: Brush.horizontalGradient(listOf(p.a, p.b)),
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

/* ---------- buttons ---------- */

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    small: Boolean = false
) {
    val p = pal()
    val shape = RoundedCornerShape(14.dp)
    val bg = if (primary) p.ink else p.card
    val fg = if (primary) p.bg else p.ink
    val borderC = if (primary) Color.Transparent else p.line
    Box(
        modifier
            .background(bg, shape)
            .border(1.dp, borderC, shape)
            .clickable { onClick() }
            .then(if (small) Modifier.padding(horizontal = 12.dp, vertical = 6.dp) else Modifier.padding(horizontal = 16.dp, vertical = 10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = Sora,
                fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = if (small) 13.sp else 15.sp
            ),
            color = fg
        )
    }
}

@Composable
fun CircleBtn(
    size: Dp,
    icon: ImageVector,
    contentDescription: String?,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val p = pal()
    Box(
        Modifier
            .size(size)
            .background(if (primary) p.ink else p.card, CircleShape)
            .border(1.dp, if (primary) Color.Transparent else p.line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (primary) p.bg else p.ink,
            modifier = Modifier.size(size * 0.4f)
        )
    }
}

/* ---------- segmented control ---------- */

@Composable
fun Seg(options: List<String>, selectedIndex: Int?, onSelect: (Int) -> Unit) {
    val p = pal()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, opt ->
            val on = i == selectedIndex
            val shape = RoundedCornerShape(14.dp)
            val bg = if (on) p.ink else p.card
            val fg = if (on) p.bg else p.ink
            Box(
                Modifier
                    .weight(1f)
                    .background(bg, shape)
                    .border(1.dp, if (on) p.ink else p.line, shape)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = fg,
                    maxLines = 1
                )
            }
        }
    }
}

/* ---------- text fields ---------- */

@Composable
fun AppTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = pal().ink,
    unfocusedTextColor = pal().ink,
    cursorColor = pal().a,
    focusedBorderColor = pal().a,
    unfocusedBorderColor = pal().line,
    focusedContainerColor = pal().inp,
    unfocusedContainerColor = pal().inp,
    focusedPlaceholderColor = pal().mut,
    unfocusedPlaceholderColor = pal().mut
)

/**
 * Text field that commits its value when focus is lost or Done is pressed,
 * mirroring the web app's onchange behavior. If [onCommit] returns false the
 * field is reset to [initial].
 */
@Composable
fun CommitTextField(
    initial: String,
    onCommit: (String) -> Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Number,
    maxLen: Int = 200
) {
    var text by remember(initial) { mutableStateOf(initial) }
    var focused by remember { mutableStateOf(false) }
    val fm = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= maxLen) text = it },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { f: FocusState ->
                if (focused && !f.isFocused) {
                    if (!onCommit(text)) text = initial
                }
                focused = f.isFocused
            },
        placeholder = { Mut(placeholder, size = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = AppTextFieldColors(),
        textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { fm.clearFocus() })
    )
}

/* ---------- misc ---------- */

@Composable
fun Dot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** lerp two colors (convenience wrapper). */
fun lerp(start: Color, stop: Color, fraction: Float): Color =
    androidx.compose.ui.graphics.lerp(start, stop, fraction.coerceIn(0f, 1f))

/** Confirm-on-second-tap button, like the web app's "Sure?" pattern. */
@Composable
fun AskButton(text: String, onConfirm: () -> Unit, small: Boolean = true, danger: Boolean = true) {
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            kotlinx.coroutines.delay(2500)
            confirming = false
        }
    }
    val p = pal()
    AppButton(
        text = if (confirming) "Sure?" else text,
        onClick = {
            if (confirming) {
                confirming = false
                onConfirm()
            } else confirming = true
        },
        small = small,
        modifier = Modifier
    )
}

@Composable
fun Chevron(open: Boolean) {
    Icon(
        Icons.Rounded.ExpandMore,
        contentDescription = if (open) "Collapse" else "Expand",
        tint = pal().mut,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
fun ItemDivider() {
    HorizontalDivider(color = pal().line, thickness = 1.dp)
}

@Composable
fun Labeled(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Mut(label, size = 12.sp)
        Spacer4()
        content()
    }
}

@Composable
fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
}
