package com.devfahim00.zerostudy.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.automirrored.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.SkipNext
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
        Column(Modifier.fillMaxWidth().widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
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

@Composable
private fun Pills() {
    val streak = Model.streak()
    val today = Model.todaySec()
    val gd = Model.S.goal.d
    val examDays = Model.examDays()
    val examName = Model.S.exam.n.ifBlank { "Exam" }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Pill(Icons.Rounded.LocalFireDepartment, "$streak ${if (streak == 1) "day" else "days"}")
        Pill(Icons.Rounded.TrackChanges, hm(today) + (if (gd > 0) " / " + hm((gd * 3600).toLong()) else ""))
        if (examDays != null && examDays >= 0) {
            Pill(
                Icons.Rounded.CalendarToday,
                if (examDays == 0L) "$examName today"
                else "$examDays ${if (examDays == 1L) "day" else "days"} to $examName"
            )
        }
    }
}

/* ---------------- timer stage ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerStage() {
    val cfg = LocalConfiguration.current
    val stage = minOf(cfg.screenWidthDp.dp * 0.76f, 360.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(stage).aspectRatio(1f)) {
            ParticleCanvas()
            RingCanvas()
            TimerCenter()
        }
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
        val stroke = size.width * 0.026f
        val trackStroke = size.width * 0.022f
        val r = size.minDimension / 2f * (46f / 50f)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = p.trk, radius = r, center = center, style = Stroke(trackStroke))
        if (fill > 0f) {
            val brush = if (!focus) SolidColor(p.ok) else Brush.sweepGradient(
                colors = listOf(p.a, p.b, p.a),
                center = center
            )
            rotate(-90f, pivot = center) {
                drawArc(
                    brush = brush,
                    startAngle = 0f,
                    sweepAngle = 360f * fill,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
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
    val sub = if (focus) {
        (if (run) "Focus" else if (e > 0) "Paused" else "Ready") + (if (t > 0) " · ${Model.S.cfg.f} min" else "")
    } else {
        (if (run) "Break" else "Break paused") + " · ${Model.S.cfg.b} min"
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
        Text(
            text = sub,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp),
            color = p.mut
        )
    }
}

/* ---------------- controls ---------------- */

@Composable
private fun Controls() {
    val focus = Model.S.ph == "f"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // stop / save / skip-break
        Box(
            Modifier
                .size(54.dp)
                .background(pal().card, CircleShape)
                .border(1.dp, pal().line, CircleShape)
                .clickable { Model.stop() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (focus) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.SkipNext,
                contentDescription = if (focus) "Save session" else "Skip break",
                tint = pal().ink,
                modifier = Modifier.size(22.dp)
            )
        }
        // start / pause
        Box(
            Modifier
                .size(72.dp)
                .background(pal().ink, CircleShape)
                .clickable { Model.toggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (Model.S.tm.run) Icons.Rounded.Pause else Icons.AutoMirrored.Rounded.PlayArrow,
                contentDescription = if (Model.S.tm.run) "Pause" else if (Model.el() > 0) "Resume" else "Start",
                tint = pal().bg,
                modifier = Modifier.size(28.dp)
            )
        }
        // fullscreen
        Box(
            Modifier
                .size(54.dp)
                .background(pal().card, CircleShape)
                .border(1.dp, pal().line, CircleShape)
                .clickable { Model.fullscreen = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Fullscreen,
                contentDescription = "Full screen",
                tint = pal().ink,
                modifier = Modifier.size(22.dp)
            )
        }
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
    val due = all.filter { it.due <= now }
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
            }
        } else {
            Mut(
                if (next != null) "All caught up. Next: ${next.ch.n} (${next.sub.n}) ${Model.whenStr(next.due)}"
                else "Mark chapters complete in Subjects and they will show up here for revision."
            )
        }
    }
}
