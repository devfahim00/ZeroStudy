package com.devfahim00.zerostudy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Chapter
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.Subject
import com.devfahim00.zerostudy.fmtNum
import com.devfahim00.zerostudy.hm
import com.devfahim00.zerostudy.wk

@Composable
fun SubjectsScreen() {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1020.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AddSubjectCard()
            if (Model.S.subs.isEmpty()) {
                AppCard { Mut("No subjects yet. Add one above, then pick it on Home before you start.") }
            } else {
                Model.S.subs.forEach { s -> SubjectCard(s) }
            }
        }
    }
}

@Composable
private fun AddSubjectCard() {
    var name by remember { mutableStateOf("") }
    AppCard {
        H2("Add a subject")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            fun tryAdd() {
                if (Model.addSubject(name)) name = ""
            }
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                placeholder = { Mut("e.g. Physics", size = 15.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = AppTextFieldColors(),
                textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { tryAdd() }),
                modifier = Modifier.weight(1f)
            )
            AppButton("Add", { tryAdd() }, primary = true)
        }
    }
}

/* ---------------- subject card ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(s: Subject) {
    val p = pal()
    val open = Model.S.open.contains(s.id)
    val color = subjectColor(s.c)

    // per-subject stats
    val stats = remember(Model.S.ses, Model.S.subs, s.id) {
        val w0 = wk(System.currentTimeMillis())
        var tot = 0.0
        var wkt = 0.0
        val ct = mutableMapOf<String, Long>()
        Model.S.ses.forEach { ses ->
            val ids = ses.s.filter { id -> Model.S.subs.any { it.id == id } }
            if (ids.contains(s.id)) {
                tot += ses.d.toDouble() / ids.size
                if (wk(ses.t) == w0) wkt += ses.d.toDouble() / ids.size
            }
            if (ses.c.isNotEmpty()) ct[ses.c] = (ct[ses.c] ?: 0L) + ses.d
        }
        Triple(tot.toLong(), wkt.toLong(), ct)
    }
    val (tot, wkt, chTime) = stats
    val doneCh = s.ch.count { it.done > 0 }

    // which optional panel is showing below the chapter list: "", "chapter" or "target"
    var panel by remember(s.id) { mutableStateOf("") }

    AppCard {
        // header: always visible, tap to expand
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { Model.setOpen(s.id, !open) }
        ) {
            Dot(color)
            Column(Modifier.weight(1f)) {
                Row {
                    Text(
                        s.n,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                        color = p.ink,
                        modifier = Modifier.alignByBaseline().weight(1f)
                    )
                    Mut("${doneCh}/${s.ch.size} chapters", size = 13.sp, modifier = Modifier.alignByBaseline())
                }
                Mut(
                    hm(tot) + " studied" + (if (s.wg > 0) " · " + hm(wkt) + " / " + fmtNum(s.wg) + "h this week" else ""),
                    size = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Bar(
                    if (s.ch.isNotEmpty()) doneCh.toFloat() / s.ch.size else 0f,
                    height = 6.dp,
                    color = color,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Chevron(open)
        }

        // body: just the chapter list plus a small action row
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(top = 12.dp)) {
                ItemDivider()
                if (s.ch.isEmpty()) {
                    Mut("No chapters yet. Tap \"Add chapter\" to start.", modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    s.ch.forEachIndexed { i, c ->
                        if (i > 0) ItemDivider()
                        ChapterRow(s, c, chTime[c.id] ?: 0L)
                    }
                }
                ItemDivider()

                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip("Add chapter", panel == "chapter", color) { panel = if (panel == "chapter") "" else "chapter" }
                    Chip("Weekly target", panel == "target", color) { panel = if (panel == "target") "" else "target" }
                    AskButton("Remove subject") { Model.removeSubject(s.id) }
                }

                AnimatedVisibility(visible = panel == "chapter") {
                    Box(Modifier.padding(top = 12.dp)) { AddChapterRow(s) }
                }
                AnimatedVisibility(visible = panel == "target") {
                    Box(Modifier.padding(top = 12.dp)) { WeeklyTarget(s, wkt) }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTarget(s: Subject, wkt: Long) {
    Column {
        Row {
            Mut("This week", size = 13.sp, modifier = Modifier.weight(1f))
            Mut(
                hm(wkt) + (if (s.wg > 0) " / " + fmtNum(s.wg) + "h" else ""),
                size = 13.sp
            )
        }
        if (s.wg > 0) {
            Bar(
                (wkt.toFloat() / (s.wg * 3600).toFloat()).coerceAtMost(1f),
                height = 8.dp,
                color = subjectColor(s.c),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        CommitTextField(
            initial = if (s.wg > 0) fmtNum(s.wg) else "",
            onCommit = { txt ->
                Model.setWeeklyGoal(s.id, txt.toDoubleOrNull() ?: 0.0)
                true
            },
            placeholder = "Weekly target (hours)",
            keyboard = KeyboardType.Decimal,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/* ---------------- chapter row ---------------- */

/**
 * Compact by default: checkbox, name and a one-line status.
 * Tap the row to reveal difficulty, note, revision progress and delete.
 */
@Composable
private fun ChapterRow(s: Subject, c: Chapter, studiedSec: Long) {
    val p = pal()
    val color = subjectColor(s.c)
    val done = c.done > 0
    var expanded by remember(c.id) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .background(if (done) color else p.card, RoundedCornerShape(8.dp))
                    .border(1.dp, if (done) color else p.line, RoundedCornerShape(8.dp))
                    .clickable { Model.toggleChapterDone(s.id, c.id) },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Unmark complete",
                        tint = Color(0xFF05060A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    c.n,
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = if (done) p.mut else p.ink
                )
                Mut(Model.chStatus(c), size = 12.sp, modifier = Modifier.padding(top = 1.dp))
            }
            Chevron(expanded, size = 20.dp)
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 38.dp, bottom = 12.dp)) {
                if (studiedSec > 0) {
                    Mut(hm(studiedSec) + " studied on this chapter", size = 12.sp)
                }
                if (done) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        repeat(Model.S.cfg.rv.size) { i ->
                            Box(
                                Modifier
                                    .width(18.dp)
                                    .height(5.dp)
                                    .background(if (i < c.rv) color else p.line, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    listOf("e" to "Easy", "m" to "Medium", "h" to "Hard").forEach { (k, label) ->
                        val on = c.dif == k
                        val kc = when (k) {
                            "e" -> p.ok
                            "m" -> p.warn
                            else -> p.red
                        }
                        Text(
                            label,
                            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
                            color = if (on) Color(0xFF05060A) else p.mut,
                            modifier = Modifier
                                .background(if (on) kc else p.card, RoundedCornerShape(99.dp))
                                .border(1.dp, if (on) Color.Transparent else p.line, RoundedCornerShape(99.dp))
                                .clickable { Model.setDiff(s.id, c.id, k) }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                NoteField(s, c)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    AskButton("Delete chapter") { Model.removeChapter(s.id, c.id) }
                }
            }
        }
    }
}

@Composable
private fun NoteField(s: Subject, c: Chapter) {
    var text by remember(c.id, c.note) { mutableStateOf(c.note) }
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        onValueChange = { if (it.length <= 140) text = it },
        placeholder = { Mut("Add a note", size = 12.sp) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = AppTextFieldColors(),
        textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .onFocusChanged { f ->
                if (focused && !f.isFocused && text.trim() != c.note) Model.setNote(s.id, c.id, text)
                focused = f.isFocused
            }
    )
}

@Composable
private fun AddChapterRow(s: Subject) {
    var name by remember(s.id) { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        fun tryAdd() {
            if (Model.addChapter(s.id, name)) name = ""
        }
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 60) name = it },
            placeholder = { Mut("Chapter name", size = 15.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldColors(),
            textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { tryAdd() }),
            modifier = Modifier.weight(1f)
        )
        AppButton("Add", { tryAdd() }, primary = true)
    }
}
