package com.devfahim00.zerostudy.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.hm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ceil

@Composable
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1020.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TimerCard()
            GoalsCard()
            ExamCard()
            ThemeCard()
            RevisionScheduleCard()
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
        H2("Timer")
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
        H2("Goals")
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
                    initial = if (Model.S.goal.d % 1.0 == 0.0) Model.S.goal.d.toLong().toString() else Model.S.goal.d.toString(),
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
                    initial = if (Model.S.goal.w % 1.0 == 0.0) Model.S.goal.w.toLong().toString() else Model.S.goal.w.toString(),
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

/* ---------------- exam countdown ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamCard() {
    var open by remember { mutableStateOf(false) }
    val n = Model.examDays()
    val eds = when {
        Model.S.exam.d.isBlank() -> "Set a date to see the countdown on Home."
        n == null -> "Set a date to see the countdown on Home."
        n < 0 -> "This exam date has passed."
        n == 0L -> "Exam is today. Good luck!"
        else -> "$n days left · about ${ceil(n / 7.0).toInt()} weeks"
    }
    val initial = try {
        if (Model.S.exam.d.isBlank()) null
        else LocalDate.parse(Model.S.exam.d).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
    AppCard {
        H2("Exam countdown")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Labeled("Exam name", Modifier.weight(1f)) {
                var name by remember(Model.S.exam.n) { mutableStateOf(Model.S.exam.n) }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 30) {
                            name = it
                            Model.setExamName(it)
                        }
                    },
                    placeholder = { Mut("e.g. Final exam", size = 15.sp) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = AppTextFieldColors(),
                    textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp)
                )
            }
            Labeled("Exam date", Modifier.weight(1f)) {
                val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(pal().inp, shape)
                        .border(1.dp, pal().line, shape)
                        .clickable { open = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Model.S.exam.d.ifBlank { "Pick a date" },
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                        color = if (Model.S.exam.d.isBlank()) pal().mut else pal().ink,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = "Pick date",
                        tint = pal().a,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Mut(eds, size = 13.sp, modifier = Modifier.padding(top = 10.dp))
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    open = false
                    val ms = state.selectedDateMillis
                    if (ms != null) {
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        Model.setExamDate(d)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        open = false
                        Model.setExamDate("")
                    }) { Text("Clear") }
                    TextButton(onClick = { open = false }) { Text("Cancel") }
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/* ---------------- theme ---------------- */

@Composable
private fun ThemeCard() {
    AppCard {
        H2("Theme")
        Seg(listOf("Dark", "Light"), if (Model.S.theme == "dark") 0 else 1) {
            Model.setTheme(if (it == 0) "dark" else "light")
        }
    }
}

/* ---------------- revision schedule ---------------- */

@Composable
private fun RevisionScheduleCard() {
    AppCard {
        H2("Revision schedule")
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
}
