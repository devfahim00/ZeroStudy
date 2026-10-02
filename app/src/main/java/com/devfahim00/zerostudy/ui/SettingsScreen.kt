package com.devfahim00.zerostudy.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Exam
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.fmtNum
import com.devfahim00.zerostudy.hm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

@Composable
fun SettingsScreen() {
    // "" = main list, otherwise the id of the open sub page
    var page by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = page.isNotEmpty()) { page = "" }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1020.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (page) {
                "timer" -> {
                    PageHeader("Timer") { page = "" }
                    TimerCard()
                }
                "goals" -> {
                    PageHeader("Goals") { page = "" }
                    GoalsCard()
                }
                "exam" -> {
                    PageHeader("Exam countdown") { page = "" }
                    ExamPage()
                }
                "revision" -> {
                    PageHeader("Revision schedule") { page = "" }
                    RevisionScheduleCard()
                }
                else -> {
                    SettingsMenu { page = it }
                    ThemeCard()
                }
            }
        }
    }
}

/* ---------------- menu ---------------- */

private data class MenuItem(val id: String, val title: String, val summary: String, val icon: ImageVector)

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    val p = pal()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(p.card, CircleShape)
                .border(1.dp, p.line, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = p.ink,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            title,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            color = p.ink
        )
    }
}

@Composable
private fun SettingsMenu(onOpen: (String) -> Unit) {
    val p = pal()
    val cfg = Model.S.cfg
    val goal = Model.S.goal
    val upcoming = Model.upcomingExams()
    val examSummary = when {
        Model.S.exams.isEmpty() -> "No exams added"
        upcoming.isEmpty() -> "${Model.S.exams.size} saved · none upcoming"
        else -> {
            val (e, d) = upcoming.first()
            (e.n.ifBlank { "Exam" }) + " in " + d + "d" +
                (if (Model.S.exams.size > 1) " · ${Model.S.exams.size} exams" else "")
        }
    }
    val items = listOf(
        MenuItem(
            "timer", "Timer",
            if (cfg.f == 0) "Free stopwatch" else "${cfg.f} min focus · ${cfg.b} min break",
            Icons.Rounded.Timer
        ),
        MenuItem(
            "goals", "Goals",
            "${fmtNum(goal.d)}h daily · ${fmtNum(goal.w)}h weekly",
            Icons.Rounded.TrackChanges
        ),
        MenuItem("exam", "Exam countdown", examSummary, Icons.Rounded.Event),
        MenuItem(
            "revision", "Revision schedule",
            cfg.rv.joinToString(", ") + " days · max " + Model.revCap() + "/day",
            Icons.Rounded.Repeat
        )
    )
    AppCard {
        items.forEachIndexed { i, item ->
            if (i > 0) ItemDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(item.id) }
                    .padding(vertical = 14.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(p.inp, RoundedCornerShape(12.dp))
                        .border(1.dp, p.line, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = p.a, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        item.title,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                        color = p.ink
                    )
                    Mut(item.summary, size = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = p.mut,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/* ---------------- theme (stays on the main list, as a toggle) ---------------- */

@Composable
private fun ThemeCard() {
    val p = pal()
    val dark = Model.S.theme == "dark"
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Dark mode",
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = p.ink
                )
                Mut(if (dark) "On" else "Off · using the light theme", size = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Switch(
                checked = dark,
                onCheckedChange = { Model.setTheme(if (it) "dark" else "light") },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = p.bg,
                    checkedTrackColor = p.a,
                    checkedBorderColor = p.a,
                    uncheckedThumbColor = p.mut,
                    uncheckedTrackColor = p.trk,
                    uncheckedBorderColor = p.line
                )
            )
        }
    }
}

/* ---------------- timer ---------------- */

@Composable
private fun TimerCard() {
    val presets = listOf(
        Triple("25 / 5", 25, 5),
        Triple("50 / 10", 50, 10),
        Triple("90 / 15", 90, 15),
        Triple("Free", 0, 0)
    )
    AppCard {
        val presetIndex = presets.indexOfFirst { it.second == Model.S.cfg.f && it.third == Model.S.cfg.b }.takeIf { it >= 0 }
        Seg(presets.map { it.first }, presetIndex) { i ->
            Model.setPreset(presets[i].second, presets[i].third)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Labeled("Session (min, 0 = free stopwatch)", Modifier.weight(1f)) {
                CommitTextField(
                    initial = Model.S.cfg.f.toString(),
                    onCommit = { txt ->
                        val v = txt.toIntOrNull()
                        if (v != null) {
                            Model.setSessionMin(v)
                            true
                        } else false
                    },
                    placeholder = "25"
                )
            }
            Labeled("Break (min)", Modifier.weight(1f)) {
                CommitTextField(
                    initial = Model.S.cfg.b.toString(),
                    onCommit = { txt ->
                        val v = txt.toIntOrNull()
                        if (v != null) {
                            Model.setBreakMin(v)
                            true
                        } else false
                    },
                    placeholder = "5"
                )
            }
        }
    }
}

/* ---------------- goals ---------------- */

@Composable
private fun GoalsCard() {
    val today = Model.todaySec()
    val week = Model.weekSec()
    val gd = Model.S.goal.d
    val gw = Model.S.goal.w
    AppCard {
        Row {
            Text(
                "Today",
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                color = pal().ink,
                modifier = Modifier.weight(1f)
            )
            Mut(hm(today) + (if (gd > 0) " / " + hm((gd * 3600).toLong()) else ""), size = 13.sp)
        }
        Bar(if (gd > 0) (today / (gd * 3600)).toFloat().coerceAtMost(1f) else 0f, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        Row(Modifier.padding(top = 12.dp)) {
            Text(
                "This week",
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                color = pal().ink,
                modifier = Modifier.weight(1f)
            )
            Mut(hm(week) + (if (gw > 0) " / " + hm((gw * 3600).toLong()) else ""), size = 13.sp)
        }
        Bar(if (gw > 0) (week / (gw * 3600)).toFloat().coerceAtMost(1f) else 0f, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
            Labeled("Daily goal (hours)", Modifier.weight(1f)) {
                CommitTextField(
                    initial = fmtNum(Model.S.goal.d),
                    onCommit = { txt ->
                        Model.setDailyGoal(txt.toDoubleOrNull() ?: 0.0)
                        true
                    },
                    placeholder = "2",
                    keyboard = KeyboardType.Decimal
                )
            }
            Labeled("Weekly goal (hours)", Modifier.weight(1f)) {
                CommitTextField(
                    initial = fmtNum(Model.S.goal.w),
                    onCommit = { txt ->
                        Model.setWeeklyGoal(txt.toDoubleOrNull() ?: 0.0)
                        true
                    },
                    placeholder = "10",
                    keyboard = KeyboardType.Decimal
                )
            }
        }
    }
}

/* ---------------- exam countdown (multiple exams) ---------------- */

private val examDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

private fun prettyDate(iso: String): String = try {
    LocalDate.parse(iso).format(examDateFmt)
} catch (e: Exception) {
    iso
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamPage() {
    val p = pal()
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }

    AppCard {
        H2("Add an exam")
        Labeled("Exam name") {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                placeholder = { Mut("e.g. Final exam", size = 15.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = AppTextFieldColors(),
                textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))
        Labeled("Exam date") {
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(p.inp, shape)
                    .border(1.dp, p.line, shape)
                    .clickable { open = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (date.isBlank()) "Pick a date" else prettyDate(date),
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = if (date.isBlank()) p.mut else p.ink,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.CalendarToday,
                    contentDescription = "Pick date",
                    tint = p.a,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        AppButton(
            "Add exam",
            {
                if (Model.addExam(name, date)) {
                    name = ""
                    date = ""
                }
            },
            primary = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    val sorted = Model.S.exams.sortedBy { it.d }
    AppCard {
        H2(if (sorted.isEmpty()) "Your exams" else "Your exams · ${sorted.size}")
        if (sorted.isEmpty()) {
            Mut("No exams yet. Add one above and the nearest ones show up on Home.")
        } else {
            sorted.forEachIndexed { i, e ->
                if (i > 0) ItemDivider()
                ExamRow(e)
            }
        }
    }

    if (open) {
        val initial = try {
            if (date.isBlank()) null
            else LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    val ms = state.selectedDateMillis
                    if (ms != null) {
                        date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ExamRow(e: Exam) {
    val p = pal()
    val n = Model.daysUntil(e.d)
    val left = when {
        n == null -> ""
        n < 0 -> "passed"
        n == 0L -> "today"
        n == 1L -> "tomorrow"
        else -> "$n days left · about ${ceil(n / 7.0).toInt()} weeks"
    }
    val past = n != null && n < 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                e.n.ifBlank { "Exam" },
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                color = if (past) p.mut else p.ink
            )
            Mut(
                prettyDate(e.d) + (if (left.isNotEmpty()) " · $left" else ""),
                size = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        AskButton("Remove") { Model.removeExam(e.id) }
    }
}

/* ---------------- revision schedule ---------------- */

@Composable
private fun RevisionScheduleCard() {
    AppCard {
        Labeled("Revise N days after completing a chapter (comma separated)") {
            CommitTextField(
                initial = Model.S.cfg.rv.joinToString(", "),
                onCommit = { txt -> Model.setRevSchedule(txt) },
                placeholder = "1, 3, 7, 15, 30",
                keyboard = KeyboardType.Number
            )
        }
        Mut(
            "Spaced repetition: each revision pushes the next one further out. If you cannot recall a chapter, tap Forgot and its cycle restarts.",
            size = 13.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
    AppCard {
        Labeled("Max revisions per day") {
            CommitTextField(
                initial = Model.revCap().toString(),
                onCommit = { txt ->
                    val v = txt.toIntOrNull()
                    if (v != null) {
                        Model.setRevCap(v)
                        true
                    } else false
                },
                placeholder = "5"
            )
        }
        Mut(
            "Chapters you mark as \"Earlier\" in Subjects are spread over the coming days using this limit. " +
                "If more revisions are due than this, the rest wait in a queue on Home and show up as you finish the others.",
            size = 13.sp,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}
