package com.devfahim00.zerostudy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.fmtNum
import com.devfahim00.zerostudy.hm
import com.devfahim00.zerostudy.p2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan
import kotlin.random.Random

@Composable
fun HomeScreen() {
    val p = pal()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Pills()
            TimerStage()
            Controls()
            Chips()
            ChapterSelect()
            Spacer(Modifier.height(14.dp))
            RevisionCard()
        }
    }
}

/* ---------------- pills ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Pills() {
    val streak = Model.streak()
    val today = Model.todaySec()
    val gd = Model.S.goal.d
    val exams = Model.upcomingExams().take(3)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Pill(Icons.Rounded.LocalFireDepartment, "$streak ${if (streak == 1) "day" else "days"}")
        Pill(Icons.Rounded.TrackChanges, hm(today) + (if (gd > 0) " / " + hm((gd * 3600).toLong()) else ""))
        exams.forEach { (exam, days) ->
            val name = exam.n.ifBlank { "Exam" }
            Pill(
                Icons.Rounded.CalendarToday,
                if (days == 0L) "$name today"
                else "$days ${if (days == 1L) "day" else "days"} to $name"
            )
        }
    }
}

/* ---------------- timer stage ---------------- */

@Composable
private fun TimerStage() {
    val cfg = LocalConfiguration.current
    val stage = minOf(cfg.screenWidthDp.dp * 0.78f, 340.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhaseBadge()
        Spacer(Modifier.height(26.dp))
        Box(Modifier.size(stage).aspectRatio(1f)) {
            ParticleCanvas()
            RingCanvas()
            TimerCenter()
        }
    }
}

/** Small status capsule above the ring: FOCUS / BREAK / PAUSED / READY. */
@Composable
private fun PhaseBadge() {
    val p = pal()
    val focus = Model.S.ph == "f"
    val run = Model.S.tm.run
    val accent = if (focus) p.a else p.ok
    val label = when {
        !focus -> if (run) "BREAK" else "BREAK PAUSED"
        run -> "FOCUS"
        Model.el() > 0 -> "PAUSED"
        else -> "READY"
    }
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(lerp(p.card, accent, 0.10f), RoundedCornerShape(99.dp))
            .border(1.dp, lerp(p.line, accent, 0.45f), RoundedCornerShape(99.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { alpha = if (run) pulse.value else 1f }
                .background(accent, CircleShape)
        )
        Text(
            label,
            style = TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp
            ),
            color = accent
        )
    }
}

/** Ambient dust ring, replicating the web app's three.js particles. */
@Composable
private fun ParticleCanvas() {
    val breakPhase = Model.S.ph == "b"
    val particles = remember {
        List(240) {
            Triple(Random.nextFloat() * 6.283f, 2.9f + (Random.nextFloat() - 0.5f) * 0.8f, (Random.nextFloat() - 0.5f) * 1.2f)
        }
    }
    var rot by remember { mutableStateOf(0f) }
    val colorMix = remember { Animatable(if (breakPhase) 1f else 0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val t = androidx.compose.runtime.withFrameNanos { it }
            if (last != 0L) rot += (t - last) / 1e9f * 0.024f
            last = t
        }
    }
    LaunchedEffect(breakPhase) {
        colorMix.animateTo(if (breakPhase) 1f else 0f, tween(900))
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.36f
                scaleY = 1.36f
            }
    ) {
        val minDim = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val k = 0.5f * minDim * 7f / 2.9f
        val tilt = 1.15f
        val cosT = cos(tilt)
        val sinT = sin(tilt)
        val col = lerp(Color(0xFF7CC4FF), Color(0xFF5EEAD4), colorMix.value)
        val camF = 1f / tan(Math.PI.toFloat() / 8f)
        particles.forEach { (angle, rad, z) ->
            val a = angle + rot
            val x = cos(a) * rad
            val y = sin(a) * rad
            val y2 = y * cosT - z * sinT
            val z2 = y * sinT + z * cosT
            val d = 7f + z2
            if (d > 0.1f) {
                val s = k / d
                val r = 0.05f * (minDim / 2f) * camF / d
                drawCircle(
                    color = col.copy(alpha = 0.35f),
                    radius = r,
                    center = Offset(cx + x * s, cy - y2 * s)
                )
            }
        }
    }
}

@Composable
private fun RingCanvas() {
    val p = pal()
    val t = Model.tgt()
    val e = Model.el()
    val focus = Model.S.ph == "f"
    val fill = if (t > 0) {
        (e.toFloat() / t.toFloat()).coerceIn(0f, 1f)
    } else {
        (e % 3_600_000L).toFloat() / 3_600_000f
    }
    Canvas(Modifier.fillMaxSize()) {
        val half = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = size.width * 0.03f
        val r = half * 0.82f
        val accent = if (focus) p.a else p.ok

        // minute-style ticks around the ring; passed ones light up with progress
        val passed = (fill * 60f).toInt()
        for (i in 0 until 60) {
            val ang = Math.toRadians(i * 6.0 - 90.0).toFloat()
            val major = i % 5 == 0
            val r0 = half * (if (major) 0.9f else 0.93f)
            val r1 = half * 0.97f
            val lit = fill > 0f && i < passed
            val c = when {
                lit -> accent.copy(alpha = if (major) 0.95f else 0.6f)
                else -> p.line.copy(alpha = if (major) 1f else 0.7f)
            }
            drawLine(
                color = c,
                start = Offset(center.x + cos(ang) * r0, center.y + sin(ang) * r0),
                end = Offset(center.x + cos(ang) * r1, center.y + sin(ang) * r1),
                strokeWidth = if (major) 2.2.dp.toPx() else 1.4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawCircle(color = p.trk, radius = r, center = center, style = Stroke(stroke))

        if (fill > 0f) {
            val brush = if (!focus) SolidColor(p.ok) else Brush.sweepGradient(
                colors = listOf(p.a, p.b, p.a),
                center = center
            )
            val topLeft = Offset(center.x - r, center.y - r)
            val arcSize = Size(r * 2f, r * 2f)
            rotate(-90f, pivot = center) {
                // soft glow under the progress arc
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = 360f * fill,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    alpha = 0.16f,
                    style = Stroke(stroke * 2.6f, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = 360f * fill,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            // knob at the head of the arc
            val ang = Math.toRadians(-90.0 + 360.0 * fill).toFloat()
            val knob = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
            drawCircle(color = p.bg, radius = stroke * 0.95f, center = knob)
            drawCircle(color = accent, radius = stroke * 0.55f, center = knob)
        }
    }
}

@Composable
private fun TimerCenter() {
    val p = pal()
    val cfg = LocalConfiguration.current
    val fontSize = (cfg.screenWidthDp * 0.09f).coerceIn(30f, 46f).sp
    val sec = Model.dispSec()
    val tm = p2((sec / 3600).toLong()) + ":" + p2((sec % 3600 / 60).toLong()) + ":" + p2((sec % 60).toLong())
    val t = Model.tgt()
    val e = Model.el()
    val focus = Model.S.ph == "f"
    val run = Model.S.tm.run
    val accent = if (focus) p.a else p.ok
    val sub = if (focus) {
        (if (run) "Focus" else if (e > 0) "Paused" else "Ready") + (if (t > 0) " · ${Model.S.cfg.f} min" else " · stopwatch")
    } else {
        (if (run) "Break" else "Break paused") + " · ${Model.S.cfg.b} min"
    }
    val names = Model.S.sel.mapNotNull { id -> Model.S.subs.find { it.id == id }?.n }
    val subject = when {
        !focus -> "Rest your eyes"
        names.isEmpty() -> "All subjects"
        names.size <= 2 -> names.joinToString(" · ")
        else -> names.first() + " +" + (names.size - 1)
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tm,
            style = TextStyle(
                fontFamily = Sora,
                fontWeight = FontWeight.Light,
                fontSize = fontSize,
                fontFeatureSettings = "tnum"
            ),
            color = p.ink
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = sub,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp),
            color = p.mut
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = subject,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
            color = accent,
            maxLines = 1,
            modifier = Modifier
                .widthIn(max = 170.dp)
                .background(lerp(p.card, accent, 0.10f), RoundedCornerShape(99.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/* ---------------- controls ---------------- */

@Composable
private fun Controls() {
    val p = pal()
    val focus = Model.S.ph == "f"
    val run = Model.S.tm.run
    val accent = if (focus) p.a else p.ok
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp, Alignment.CenterHorizontally)
    ) {
        SideAction(
            icon = if (focus) Icons.Rounded.Check else Icons.Rounded.SkipNext,
            label = if (focus) "Save" else "Skip",
            desc = if (focus) "Save session" else "Skip break"
        ) { Model.stop() }

        // main start / pause button with a soft halo
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(accent.copy(alpha = if (run) 0.16f else 0.08f), CircleShape)
            )
            Box(
                Modifier
                    .size(76.dp)
                    .background(
                        Brush.linearGradient(if (focus) listOf(p.a, p.b) else listOf(p.ok, p.a)),
                        CircleShape
                    )
                    .clickable { Model.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (run) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (run) "Pause" else if (Model.el() > 0) "Resume" else "Start",
                    tint = Color(0xFF05060A),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        SideAction(
            icon = Icons.Rounded.Fullscreen,
            label = "Full screen",
            desc = "Full screen"
        ) { Model.fullscreen = true }
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, desc: String, onClick: () -> Unit) {
    val p = pal()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .background(p.card, CircleShape)
                .border(1.dp, p.line, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = desc, tint = p.ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Mut(label, size = 11.sp)
    }
}

/* ---------------- subject chips + chapter select ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips() {
    Column(Modifier.padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("All subjects", Model.S.sel.isEmpty()) { Model.toggleSel(null) }
            Model.S.subs.forEach { s ->
                Chip(s.n, Model.S.sel.contains(s.id), subjectColor(s.c)) { Model.toggleSel(s.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelect() {
    val one = if (Model.S.sel.size == 1) Model.S.subs.find { it.id == Model.S.sel[0] } else null
    val chapters = one?.ch ?: emptyList()
    if (one == null || chapters.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val selected = chapters.find { it.id == Model.S.sc }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .padding(top = 12.dp)
            .width(320.dp)
    ) {
        OutlinedTextField(
            value = selected?.n ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Mut("Chapter (optional)", size = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldColors(),
            textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Mut("Chapter (optional)", size = 14.sp) },
                onClick = { Model.setSc(""); expanded = false }
            )
            chapters.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.n, style = TextStyle(fontFamily = Sora, fontSize = 14.sp), color = pal().ink) },
                    onClick = { Model.setSc(c.id); expanded = false }
                )
            }
        }
    }
}

/* ---------------- revision card ---------------- */

@Composable
fun RevisionCard() {
    val p = pal()
    val all = Model.dueItems()
    val now = System.currentTimeMillis()
    val dueAll = all.filter { it.due <= now }
    val due = dueAll.take(Model.revCap())
    val queued = dueAll.size - due.size
    val next = all.firstOrNull { it.due > now }
    AppCard {
        H2(if (due.isNotEmpty()) "Revise today · ${due.size}" else "Revision")
        if (due.isNotEmpty()) {
            Column {
                due.forEachIndexed { i, x ->
                    if (i > 0) ItemDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Dot(subjectColor(x.sub.c))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                x.ch.n,
                                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                                color = p.ink
                            )
                            Mut(
                                "${x.sub.n} · Revision ${x.ch.rv + 1}/${Model.S.cfg.rv.size} · " +
                                    Model.whenStr(x.due),
                                size = 13.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppButton("Done", { Model.revise(x.sub.id, x.ch.id, true) }, small = true)
                            AppButton("Forgot", { Model.revise(x.sub.id, x.ch.id, false) }, small = true)
                        }
                    }
                }
                if (queued > 0) {
                    ItemDivider()
                    Mut(
                        "+$queued more waiting in the queue. They appear as you finish these.",
                        size = 12.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        } else {
            Mut(
                if (next != null) "All caught up. Next: ${next.ch.n} (${next.sub.n}) ${Model.whenStr(next.due)}"
                else "Mark chapters complete in Subjects and they will show up here for revision."
            )
        }
    }
}
