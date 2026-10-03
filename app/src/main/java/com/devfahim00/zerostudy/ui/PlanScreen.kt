package com.devfahim00.zerostudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.PlanItem
import com.devfahim00.zerostudy.fmtDate
import com.devfahim00.zerostudy.fmtWeekday

/** Today's plan: a small checklist for the day, plus the revisions that are due and your level. */
@Composable
fun PlanScreen() {
    val p = pal()
    val nowMs = Model.now
    val today = Model.planToday()
    val carried = Model.planCarried()
    val doneN = today.count { it.done }
    val dueNow = Model.dueNow().take(Model.revCap())
    val level = Model.levelOf(Model.S.xp)
    val (inLevel, levelNeed) = Model.levelProgress()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // summary
            AppCard {
                H2("Today's plan")
                Text(
                    fmtWeekday(nowMs) + ", " + fmtDate(nowMs),
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    color = p.ink
                )
                Mut(
                    if (today.isEmpty()) "No tasks yet" else "$doneN of ${today.size} tasks done",
                    size = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )
                Bar(if (today.isEmpty()) 0f else doneN.toFloat() / today.size, color = p.ok)
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        "Revisions", dueNow.size.toString(),
                        if (dueNow.isEmpty()) "none due today" else "due today",
                        Modifier.weight(1f).fillMaxHeight()
                    )
                    StatBox(
                        "Level", level.toString(), "$inLevel / $levelNeed XP",
                        Modifier.weight(1f).fillMaxHeight(),
                        progress = if (levelNeed > 0) inLevel.toFloat() / levelNeed else 0f
                    )
                }
            }

            AddTaskCard()

            // today's tasks
            AppCard {
                H2(if (today.isEmpty()) "Tasks" else "Tasks · $doneN/${today.size}")
                if (today.isEmpty()) {
                    Mut("Nothing planned yet. Add what you want to finish today, then tick it off.")
                } else {
                    // unfinished first, finished at the bottom
                    val ordered = today.filter { !it.done } + today.filter { it.done }
                    ordered.forEachIndexed { i, item ->
                        if (i > 0) ItemDivider()
                        TaskRow(item)
                    }
                }
            }

            // unfinished from earlier days
            if (carried.isNotEmpty()) {
                AppCard {
                    H2("Carried over · ${carried.size}")
                    Mut(
                        "Not finished earlier. Tick one to count it for today.",
                        size = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    carried.forEachIndexed { i, item ->
                        if (i > 0) ItemDivider()
                        TaskRow(item, carried = true)
                    }
                }
            }

            // revisions that are due today, so the whole day is in one place
            AppCard {
                H2(if (dueNow.isEmpty()) "Revisions" else "Revisions due today · ${dueNow.size}")
                if (dueNow.isEmpty()) {
                    Mut("No revisions due today.")
                } else {
                    val total = Model.S.cfg.rv.size
                    dueNow.forEachIndexed { i, x ->
                        if (i > 0) ItemDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            Dot(subjectColor(x.sub.c), 9.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    x.ch.n,
                                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                                    color = p.ink
                                )
                                Mut("${x.sub.n} · Revision ${x.ch.rv + 1}/$total", size = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AppButton("Done", { Model.revise(x.sub.id, x.ch.id, true) }, small = true)
                                AppButton("Forgot", { Model.revise(x.sub.id, x.ch.id, false) }, small = true)
                            }
                        }
                    }
                }
            }
            Box(Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun AddTaskCard() {
    val p = pal()
    var text by remember { mutableStateOf("") }
    var subId by remember { mutableStateOf("") }
    val fm = LocalFocusManager.current
    fun submit() {
        if (Model.addPlan(text, subId)) {
            text = ""
            fm.clearFocus()
        }
    }
    AppCard {
        H2("Add a task")
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= 80) text = it },
            placeholder = { Mut("e.g. Physics chapter 3 MCQs", size = 15.sp) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = AppTextFieldColors(),
            textStyle = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus()
        )
        if (Model.S.subs.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                Chip("No subject", subId.isEmpty()) { subId = "" }
                Model.S.subs.forEach { sub ->
                    Chip(sub.n, subId == sub.id, subjectColor(sub.c)) { subId = if (subId == sub.id) "" else sub.id }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AppButton("Add to today", { submit() }, primary = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TaskRow(item: PlanItem, carried: Boolean = false) {
    val p = pal()
    val sub = Model.S.subs.find { it.id == item.sub }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        TickBox(item.done) { Model.togglePlan(item.id) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.t,
                style = TextStyle(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.done) p.mut else p.ink
            )
            if (sub != null || carried) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (sub != null) {
                        Dot(subjectColor(sub.c), 8.dp)
                        Mut(sub.n, size = 12.sp)
                    }
                    if (carried) Mut(if (sub != null) "· earlier" else "Earlier", size = 12.sp)
                }
            }
        }
        Box(
            Modifier
                .size(34.dp)
                .clickable { Model.removePlan(item.id) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove task", tint = p.mut, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TickBox(on: Boolean, onClick: () -> Unit) {
    val p = pal()
    Box(
        Modifier
            .size(26.dp)
            .background(if (on) p.ok else Color.Transparent, CircleShape)
            .border(2.dp, if (on) p.ok else p.line, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (on) Icon(Icons.Rounded.Check, contentDescription = "Done", tint = p.bg, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatBox(label: String, value: String, sub: String, modifier: Modifier = Modifier, progress: Float? = null) {
    val p = pal()
    Column(
        modifier
            .background(p.inp, RoundedCornerShape(14.dp))
            .border(1.dp, p.line, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Mut(label, size = 11.sp)
        Text(
            value,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            color = p.ink,
            modifier = Modifier.padding(top = 2.dp)
        )
        Mut(sub, size = 11.sp)
        if (progress != null) {
            Spacer(Modifier.weight(1f))
            Bar(progress, height = 5.dp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}
