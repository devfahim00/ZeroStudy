package com.devfahim00.zerostudy.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import com.devfahim00.zerostudy.Chapter
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.devfahim00.zerostudy.AmbientSounds
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // On normal phones everything fits in one screen (the timer takes the leftover space).
        // Only on very short screens does the page fall back to scrolling.
        val fits = maxHeight >= 600.dp
        val tall = maxHeight >= 720.dp
        val base = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)
        Column(
            if (fits) base else base.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .then(if (fits) Modifier.fillMaxHeight() else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar()
                TimerStage(if (fits) Modifier.weight(1f).heightIn(min = 150.dp) else Modifier.padding(vertical = 12.dp), fits)
                Controls()
                Selectors()
                Spacer(Modifier.height(10.dp))
                TodayCard()
                Spacer(Modifier.height(8.dp))
                RevisionCard(if (tall) 2 else 1)
            }
        }
    }
}

/* ---------------- top bar ---------------- */

/** One slim row: streak on the left, the sound switch on the right. */
@Composable
private fun TopBar() {
    val streak = Model.streak()
    val amb = Model.S.amb
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Pill(Icons.Rounded.LocalFireDepartment, "$streak")
        Pill(
            if (amb == "off") Icons.Rounded.VolumeOff else Icons.Rounded.MusicNote,
            if (amb == "off") "Off" else AmbientSounds.byId(amb).name,
            onClick = { Model.cycleAmbient() }
        )
    }
}

/* ---------------- today card ---------------- */

/** Focus goal, level and the next exam as three compact blocks in one card. */
@Composable
private fun TodayCard() {
    val p = pal()
    val today = Model.todaySec()
    val gd = Model.S.goal.d
    val goalSec = (gd * 3600).toLong()
    val level = Model.levelOf(Model.S.xp)
    val (inLevel, levelNeed) = Model.levelProgress()
    val exams = Model.upcomingExams()
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.card, RoundedCornerShape(20.dp))
            .border(1.dp, p.line, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StatBlock(
            Icons.Rounded.TrackChanges, "Focus",
            hm(today) + (if (gd > 0) " / " + hm(goalSec) else ""),
            if (gd > 0 && goalSec > 0) today.toFloat() / goalSec else null,
            Modifier.weight(1f)
        )
        StatBlock(
            Icons.Rounded.Star, "Level $level", "$inLevel / $levelNeed XP",
            if (levelNeed > 0) inLevel.toFloat() / levelNeed else 0f,
            Modifier.weight(1f)
        )
        if (exams.isNotEmpty()) {
            val (exam, days) = exams.first()
            StatBlock(
                Icons.Rounded.CalendarToday,
                exam.n.ifBlank { "Exam" },
                if (days == 0L) "Today" else "$days ${if (days == 1L) "day" else "days"}",
                null,
                Modifier.weight(1f),
                foot = if (exams.size > 1) "+${exams.size - 1} more" else null
            )
        }
    }
}

@Composable
private fun StatBlock(
    icon: ImageVector,
    title: String,
    value: String,
    progress: Float?,
    modifier: Modifier = Modifier,
    foot: String? = null
) {
    val p = pal()
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, contentDescription = null, tint = p.a, modifier = Modifier.size(13.dp))
            Mut(title, size = 11.sp, modifier = Modifier.weight(1f, fill = false))
        }
        Text(
            value,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            color = p.ink,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp)
        )
        if (progress != null) {
            Bar(progress, height = 5.dp, modifier = Modifier.padding(top = 7.dp))
        } else if (foot != null) {
            Mut(foot, size = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/* ---------------- timer stage ---------------- */

@Composable
private fun TimerStage(modifier: Modifier, fits: Boolean) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val stage = if (fits) minOf(maxWidth * 0.82f, maxHeight, 340.dp) else minOf(maxWidth * 0.72f, 250.dp)
        Box(Modifier.size(stage).aspectRatio(1f)) {
            ParticleCanvas()
            RingCanvas()
            TimerCenter(stage)
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
private fun TimerCenter(stage: Dp) {
    val p = pal()
    val fontSize = (stage.value * 0.135f).coerceIn(24f, 46f).sp
    val sec = Model.dispSec()
    val tm = p2((sec / 3600).toLong()) + ":" + p2((sec % 3600 / 60).toLong()) + ":" + p2((sec % 60).toLong())
    val t = Model.tgt()
    val e = Model.el()
    val focus = Model.S.ph == "f"
    val run = Model.S.tm.run
    val accent = if (focus) p.a else p.ok
    val sub = if (focus) {
        if (run || e > 0) ((if (run) "Focus" else "Paused") + (if (t > 0) " · ${Model.S.cfg.f} min" else " · stopwatch")) else (if (t > 0) "${Model.S.cfg.f} min" else "Stopwatch")
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally)
    ) {
        SideAction(
            icon = if (focus) Icons.Rounded.Check else Icons.Rounded.SkipNext,
            desc = if (focus) "Save session" else "Skip break"
        ) { Model.stop() }

        // main start / pause button with a soft halo
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(accent.copy(alpha = if (run) 0.16f else 0.08f), CircleShape)
            )
            Box(
                Modifier
                    .size(66.dp)
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
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        SideAction(icon = Icons.Rounded.Fullscreen, desc = "Full screen") { Model.fullscreen = true }
    }
}

@Composable
private fun SideAction(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val p = pal()
    Box(
        Modifier
            .size(48.dp)
            .background(p.card, CircleShape)
            .border(1.dp, p.line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = p.ink, modifier = Modifier.size(21.dp))
    }
}

/* ---------------- subject + chapter selectors ---------------- */

/** Subject and (when one subject is picked) chapter dropdowns side by side in one row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Selectors() {
    val locked = Model.subjectLocked()
    val subs = Model.S.subs
    val sel = Model.S.sel
    val picked = subs.filter { sel.contains(it.id) }
    val one = if (sel.size == 1) subs.find { it.id == sel[0] } else null
    val chapters = one?.ch ?: emptyList()
    val showChapter = one != null && chapters.isNotEmpty()
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SubjectDropdown(picked.map { it.id }, locked, Modifier.weight(1f))
            if (showChapter) ChapterDropdown(chapters, locked, Modifier.weight(1f))
        }
        // picked subjects as one swipeable row (drag side to side when there are many)
        if (picked.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                picked.forEach { sub ->
                    Chip(sub.n, true, subjectColor(sub.c), !locked) { Model.toggleSel(sub.id) }
                }
            }
        }
        if (locked) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = pal().mut, modifier = Modifier.size(12.dp))
                Mut("Locked until you save or reset this session", size = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectDropdown(pickedIds: List<String>, locked: Boolean, modifier: Modifier) {
    val subs = Model.S.subs
    val sel = Model.S.sel
    val picked = subs.filter { pickedIds.contains(it.id) }
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        picked.isEmpty() -> "All subjects"
        picked.size == 1 -> picked[0].n
        else -> "${picked.size} subjects"
    }
    ExposedDropdownMenuBox(
        expanded = expanded && !locked,
        onExpandedChange = { if (!locked) expanded = it },
        modifier = modifier.graphicsLayer { alpha = if (locked) 0.45f else 1f }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && !locked) },
            textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        // multi-select: the menu stays open so several subjects can be ticked
        ExposedDropdownMenu(expanded = expanded && !locked, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        "All subjects",
                        style = TextStyle(fontFamily = Sora, fontSize = 14.sp),
                        color = if (sel.isEmpty()) pal().a else pal().ink
                    )
                },
                trailingIcon = {
                    if (sel.isEmpty()) Icon(Icons.Rounded.Check, contentDescription = null, tint = pal().a, modifier = Modifier.size(18.dp))
                },
                onClick = { Model.toggleSel(null); expanded = false }
            )
            subs.forEach { sub ->
                val on = sel.contains(sub.id)
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Dot(subjectColor(sub.c), 10.dp)
                            Text(
                                sub.n,
                                style = TextStyle(fontFamily = Sora, fontSize = 14.sp),
                                color = if (on) pal().a else pal().ink
                            )
                        }
                    },
                    trailingIcon = {
                        if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = pal().a, modifier = Modifier.size(18.dp))
                    },
                    onClick = { Model.toggleSel(sub.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterDropdown(chapters: List<Chapter>, locked: Boolean, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val selected = chapters.find { it.id == Model.S.sc }
    ExposedDropdownMenuBox(
        expanded = expanded && !locked,
        onExpandedChange = { if (!locked) expanded = it },
        modifier = modifier.graphicsLayer { alpha = if (locked) 0.45f else 1f }
    ) {
        OutlinedTextField(
            value = selected?.n ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Mut("Chapter", size = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldColors(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && !locked) },
            textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded && !locked, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Mut("No chapter", size = 14.sp) },
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
fun RevisionCard(maxItems: Int = 2) {
    val p = pal()
    val all = Model.dueItems()
    val now = System.currentTimeMillis()
    val dueAll = all.filter { it.due <= now }
    val due = dueAll.take(Model.revCap()).take(maxItems)
    val more = dueAll.size - due.size
    val next = all.firstOrNull { it.due > now }
    AppCard(padding = 14.dp) {
        H2(if (dueAll.isNotEmpty()) "Revise today · ${dueAll.size}" else "Revision", Modifier.padding(bottom = 0.dp))
        if (due.isNotEmpty()) {
            Column {
                due.forEachIndexed { i, x ->
                    if (i > 0) ItemDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 7.dp)
                    ) {
                        Dot(subjectColor(x.sub.c))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                x.ch.n,
                                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                                color = p.ink,
                                maxLines = 1
                            )
                            Mut(
                                "${x.sub.n} · Revision ${x.ch.rv + 1}/${Model.S.cfg.rv.size}",
                                size = 12.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppButton("Done", { Model.revise(x.sub.id, x.ch.id, true) }, small = true)
                            AppButton("Forgot", { Model.revise(x.sub.id, x.ch.id, false) }, small = true)
                        }
                    }
                }
                if (more > 0) {
                    Mut("+$more more · open the Revisions tab", size = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        } else {
            Mut(
                if (next != null) "All caught up. Next: ${next.ch.n} (${next.sub.n}) ${Model.whenStr(next.due)}"
                else "Mark chapters complete in Subjects and they will show up here for revision.",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
