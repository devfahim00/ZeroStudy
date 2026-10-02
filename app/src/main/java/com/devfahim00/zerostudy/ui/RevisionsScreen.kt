package com.devfahim00.zerostudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.dayDiff
import com.devfahim00.zerostudy.fmtDate
import com.devfahim00.zerostudy.fmtWeekday

/**
 * Revision schedule: every upcoming revision grouped by date, then by subject, then chapter.
 * Today's card (overdue included) holds the chapters that fit under the daily cap, with
 * Done / Forgot buttons; anything above the cap waits in a queue.
 */
@Composable
fun RevisionsScreen() {
    val p = pal()
    var filter by rememberSaveable { mutableStateOf("") }
    val nowMs = System.currentTimeMillis()
    val cap = Model.revCap()
    val total = Model.S.cfg.rv.size

    val everything = Model.dueItems()
    val dueAll = everything.filter { it.due <= nowMs }
    val visibleIds = dueAll.take(cap).map { it.ch.id }.toSet()

    fun keep(x: Model.DueItem) = filter.isEmpty() || x.sub.id == filter
    val today = dueAll.filter(::keep)
    val future = everything.filter { it.due > nowMs }.filter(::keep)
    val byDay = future.groupBy { dayDiff(it.due, nowMs) }.toSortedMap()
    val next7 = everything.count { it.due > nowMs && dayDiff(it.due, nowMs) <= 7 }

    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // summary
            AppCard {
                H2("Revision schedule")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SumBox("Today", Model.dueShownCount().toString(), if (dueAll.size > cap) "+${dueAll.size - cap} waiting" else "max $cap / day", Modifier.weight(1f))
                    SumBox("Next 7 days", next7.toString(), "revisions", Modifier.weight(1f))
                    SumBox("Scheduled", everything.size.toString(), "in total", Modifier.weight(1f))
                }
            }

            // subject filter (swipe sideways when there are many)
            if (Model.S.subs.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    Chip("All", filter.isEmpty()) { filter = "" }
                    Model.S.subs.forEach { sub ->
                        Chip(sub.n, filter == sub.id, subjectColor(sub.c)) { filter = if (filter == sub.id) "" else sub.id }
                    }
                }
            }

            if (everything.isEmpty()) {
                AppCard {
                    Mut("Nothing scheduled yet. Mark a chapter complete in Subjects and its revision dates will appear here, spread so no day goes over your daily limit.")
                }
            } else {
                // today (overdue included)
                if (today.isNotEmpty()) {
                    val shown = today.filter { it.ch.id in visibleIds }
                    val waiting = today.filter { it.ch.id !in visibleIds }
                    DayCard(
                        title = "Today",
                        sub = fmtWeekday(nowMs) + ", " + fmtDate(nowMs),
                        count = "${shown.size}/$cap",
                        full = shown.size >= cap
                    ) {
                        SubjectGroups(shown, actions = true)
                        if (waiting.isNotEmpty()) {
                            Mut(
                                "Waiting in queue · ${waiting.size}",
                                size = 12.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                            )
                            SubjectGroups(waiting, actions = false, dim = true)
                        }
                    }
                } else if (filter.isEmpty() || dueAll.isEmpty()) {
                    DayCard("Today", fmtWeekday(nowMs) + ", " + fmtDate(nowMs), "0/$cap", false) {
                        Mut("No revisions due today.")
                    }
                }

                byDay.forEach { (day, items) ->
                    val date = items.first().due
                    DayCard(
                        title = if (day == 1L) "Tomorrow" else fmtWeekday(date) + ", " + fmtDate(date),
                        sub = if (day == 1L) fmtWeekday(date) + ", " + fmtDate(date) else "in $day days",
                        count = "${items.size}/$cap",
                        full = items.size >= cap
                    ) {
                        SubjectGroups(items, actions = false)
                    }
                }

                if (future.isEmpty() && dueAll.isNotEmpty() && filter.isEmpty()) {
                    Mut("Finish today's revisions and the next ones will show up here.", size = 12.sp)
                }
            }
            Box(Modifier.padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun SumBox(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
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
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
            color = p.ink,
            modifier = Modifier.padding(top = 2.dp)
        )
        Mut(sub, size = 11.sp)
    }
}

@Composable
private fun DayCard(title: String, sub: String, count: String, full: Boolean, content: @Composable () -> Unit) {
    val p = pal()
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                    color = p.ink
                )
                Mut(sub, size = 12.sp)
            }
            Tag(count, if (full) p.a else p.mut)
        }
        content()
    }
}

@Composable
private fun SubjectGroups(items: List<Model.DueItem>, actions: Boolean, dim: Boolean = false) {
    val p = pal()
    val total = Model.S.cfg.rv.size
    val groups = items.groupBy { it.sub.id }
    var first = true
    groups.forEach { (_, list) ->
        val sub = list.first().sub
        val col = subjectColor(sub.c)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = if (first) 6.dp else 14.dp, bottom = 2.dp)
        ) {
            Dot(col, 9.dp)
            Text(
                sub.n,
                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = if (dim) p.mut else col
            )
            Mut("${list.size}", size = 12.sp)
        }
        first = false
        list.forEachIndexed { i, x ->
            if (i > 0) ItemDivider()
            val overdue = dayDiff(x.due, System.currentTimeMillis()) < 0
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f).padding(start = 17.dp)) {
                    Text(
                        x.ch.n,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                        color = if (dim) p.mut else p.ink
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Mut("Revision ${x.ch.rv + 1}/$total", size = 12.sp)
                        if (overdue) Tag(Model.whenStr(x.due), p.red)
                    }
                }
                if (actions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppButton("Done", { Model.revise(x.sub.id, x.ch.id, true) }, small = true)
                        AppButton("Forgot", { Model.revise(x.sub.id, x.ch.id, false) }, small = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text,
        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        color = color,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
