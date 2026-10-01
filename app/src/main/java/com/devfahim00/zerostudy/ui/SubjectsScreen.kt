package com.devfahim00.zerostudy.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
            val fm = androidx.compose.ui.platform.LocalFocusManager.current
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

    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.clickable { Model.setOpen(s.id, !open) }
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
        AnimatedVisibility(visible = open) {
            Column(Modifier.padding(top = 14.dp)) {
                WeeklyTarget(s, wkt)
                Spacer(Modifier.height(12.dp))
                if (s.ch.isEmpty()) {
                    Mut("No chapters yet.")
                } else {
                    Column {
                        s.ch.forEachIndexed { i, c ->
                            if (i > 0) ItemDivider()
                            ChapterRow(s, c, chTime[c.id] ?: 0L)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                AddChapterRow(s)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AskButton("Remove subject") { Model.removeSubject(s.id) }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTarget(s: Subject, wkt: Long) {
    Column {
        Row {
            Mut("Weekly target", size = 13.sp, modifier = Modifier.weight(1f))
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
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/* ---------------- chapter row ---------------- */

@Composable
private fun ChapterRow(s: Subject, c: Chapter, studiedSec: Long) {
    val p = pal()
    val color = subjectColor(s.c)
    val done = c.done > 0
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            // checkbox
            Box(
                Modifier
                    .size(28.dp)
                    .background(if (done) color else p.card, RoundedCornerShape(8.dp))
                    .border(1.dp, if (done) color else p.line, RoundedCornerShape(8.dp))
                    .clickable { Model.toggleChapterDone(s.id, c.id) },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = if (done) "Unmark complete" else "Mark complete",
                        tint = Color(0xFF05060A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    c.n,
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                    color = if (done) p.mut else p.ink
                )
                Mut(
                    Model.chStatus(c) + (if (studiedSec > 0) " · " + hm(studiedSec) + " studied" else ""),
                    size = 13.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
                if (done && c.rv > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 5.dp)) {
                        repeat(Model.S.cfg.rv.size) { i ->
                            Box(
                                Modifier
                                    .width(16.dp)
                                    .height(5.dp)
                                    .background(if (i < c.rv) color else p.line, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
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
                            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 11.sp),
                            color = if (on) Color(0xFF05060A) else p.mut,
                            modifier = Modifier
                                .background(if (on) kc else p.card, RoundedCornerShape(99.dp))
                                .border(1.dp, if (on) Color.Transparent else p.line, RoundedCornerShape(99.dp))
                                .clickable { Model.setDiff(s.id, c.id, k) }
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                NoteField(s, c)
            }
            ConfirmDeleteIcon { Model.removeChapter(s.id, c.id) }
        }
    }
}

/** Web-style "Sure?" confirmation on the chapter delete button. */
@Composable
private fun ConfirmDeleteIcon(onConfirm: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            kotlinx.coroutines.delay(2500)
            confirming = false
        }
    }
    if (confirming) {
        Text(
            "Sure?",
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 13.sp),
            color = pal().red,
            modifier = Modifier
                .clickable {
                    confirming = false
                    onConfirm()
                }
                .padding(horizontal = 6.dp)
        )
    } else {
        Box(
            Modifier
                .size(28.dp)
                .clickable { confirming = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Delete chapter",
                tint = pal().mut,
                modifier = Modifier.size(15.dp)
            )
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
        placeholder = { Mut("Note", size = 12.sp) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = AppTextFieldColors(),
        textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
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
            placeholder = { Mut("Add chapter", size = 15.sp) },
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
