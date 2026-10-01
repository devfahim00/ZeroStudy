package com.devfahim00.zerostudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devfahim00.zerostudy.Model
import com.devfahim00.zerostudy.Session
import com.devfahim00.zerostudy.Subject
import com.devfahim00.zerostudy.dkey
import com.devfahim00.zerostudy.fmtClock
import com.devfahim00.zerostudy.fmtDate
import com.devfahim00.zerostudy.fmtWeekday
import com.devfahim00.zerostudy.hm
import com.devfahim00.zerostudy.sod
import com.devfahim00.zerostudy.wk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

private class Bucket(val label: String, val start: Long, val end: Long) {
    var v: Long = 0
}

@Composable
fun StatsScreen() {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1020.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Seg(listOf("Daily", "Weekly", "Monthly"), listOf("d", "w", "m").indexOf(Model.statsRange).takeIf { it >= 0 }) {
                Model.statsRange = listOf("d", "w", "m")[it]
            }
            SummaryCard()
            ChartCard()
            val wide = LocalConfiguration.current.screenWidthDp >= 600
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(1f)) { SubjectProgressCard() }
                    Box(Modifier.weight(1f)) { TodayCard() }
                }
            } else {
                SubjectProgressCard()
                TodayCard()
            }
            HeatmapCard()
            BestCard()
        }
    }
}

/* ---------------- buckets ---------------- */

private fun buildBuckets(range: String): List<Bucket> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val out = mutableListOf<Bucket>()
    when (range) {
        "d" -> {
            val s0 = today.atStartOfDay(zone).toInstant().toEpochMilli()
            for (h in 0 until 24) {
                out.add(Bucket(if (h % 6 == 0) "${h}h" else "", s0 + h * 3_600_000L, s0 + (h + 1) * 3_600_000L))
            }
        }
        "w" -> {
            for (i in 6 downTo 0) {
                val d = today.minusDays(i.toLong())
                val s = d.atStartOfDay(zone).toInstant().toEpochMilli()
                out.add(Bucket(fmtWeekday(s), s, s + 86_400_000L))
            }
        }
        else -> {
            for (i in 29 downTo 0) {
                val d = today.minusDays(i.toLong())
                val s = d.atStartOfDay(zone).toInstant().toEpochMilli()
                out.add(Bucket(if (i % 5 == 0) "${d.dayOfMonth}" else "", s, s + 86_400_000L))
            }
        }
    }
    return out
}

private fun rangeSessions(range: String): List<Session> {
    val b = buildBuckets(range)
    val rs = b.first().start
    val re = b.last().end
    return Model.S.ses.filter { it.t >= rs && it.t < re }
}

/* ---------------- summary ---------------- */

@Composable
private fun SummaryCard() {
    val list = rangeSessions(Model.statsRange)
    val total = list.sumOf { it.d.toLong() }
    val cnt = list.size
    val avg = if (cnt > 0) hm(total / cnt) else "–"
    val longest = if (cnt > 0) hm(list.maxOf { it.d.toLong() }) else "–"
    AppCard {
        val wide = LocalConfiguration.current.screenWidthDp >= 600
        val cells = listOf(
            "Total" to hm(total),
            "Sessions" to cnt.toString(),
            "Average session" to avg,
            "Longest session" to longest
        )
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cells.forEach { (k, v) -> SumCell(k, v, Modifier.weight(1f)) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SumCell(cells[0].first, cells[0].second, Modifier.weight(1f))
                SumCell(cells[1].first, cells[1].second, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SumCell(cells[2].first, cells[2].second, Modifier.weight(1f))
                SumCell(cells[3].first, cells[3].second, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SumCell(label: String, value: String, modifier: Modifier = Modifier) {
    val p = pal()
    Column(modifier) {
        Mut(label, size = 13.sp)
        Text(
            value,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            color = p.ink
        )
    }
}

/* ---------------- bar chart ---------------- */

@Composable
private fun ChartCard() {
    val p = pal()
    val range = Model.statsRange
    val buckets = buildBuckets(range)
    val list = rangeSessions(range)
    list.forEach { s ->
        buckets.find { s.t >= it.start && s.t < it.end }?.let { it.v += s.d }
    }
    val mx = max(1L, buckets.maxOf { it.v })
    val title = when (range) {
        "d" -> "Today by hour"
        "w" -> "Last 7 days"
        else -> "Last 30 days"
    }
    AppCard {
        H2(title)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth().height(150.dp)
        ) {
            buckets.forEach { b ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(((b.v.toFloat() / mx) * 0.86f * 150f).coerceAtLeast(2f).dp)
                            .background(
                                Brush.verticalGradient(listOf(p.a, p.b)),
                                RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
                            )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        b.label,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 10.sp),
                        color = p.mut,
                        maxLines = 1,
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
        }
    }
}

/* ---------------- subject progress ---------------- */

private class SubRow(val id: String, val name: String, val color: Color, val v: Long, val chapters: Int, val doneCh: Int, val real: Boolean)

@Composable
private fun SubjectProgressCard() {
    val p = pal()
    val range = Model.statsRange
    val b = buildBuckets(range)
    val list = rangeSessions(range)
    val per = mutableMapOf<String, Double>()
    list.forEach { s ->
        val ids = s.s.filter { id -> Model.S.subs.any { it.id == id } }
        val eff = if (ids.isNotEmpty()) ids else listOf("_")
        eff.forEach { per[it] = (per[it] ?: 0.0) + s.d.toDouble() / eff.size }
    }
    val rows = Model.S.subs.map { SubRow(it.id, it.n, subjectColor(it.c), (per[it.id] ?: 0.0).toLong(), it.ch.size, it.ch.count { c -> c.done > 0 }, true) }.toMutableList()
    if ((per["_"] ?: 0.0) > 0) rows.add(SubRow("_", "General", p.mut, per["_"]!!.toLong(), 0, 0, false))
    rows.sortByDescending { it.v }
    val nz = rows.filter { it.real && it.v > 0 }
    val most = nz.firstOrNull()?.id
    val least = if (nz.size > 1) nz.last().id else null
    val tt = rows.sumOf { it.v }
    val top = max(1L, rows.maxOf { it.v })

    val lastStudied = remember(Model.S.ses) {
        val m = mutableMapOf<String, Long>()
        Model.S.ses.forEach { s -> s.s.filter { id -> Model.S.subs.any { it.id == id } }.forEach { m[it] = max(m[it] ?: 0L, s.t) } }
        m
    }

    AppCard {
        H2("Subject progress")
        if (rows.isEmpty()) {
            Mut("Add subjects to compare them.")
        } else {
            if (nz.size > 1) {
                Row {
                    Mut("Most: ", size = 13.sp)
                    Text(
                        nz[0].name,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = p.ink
                    )
                    Mut(" · Least: ", size = 13.sp)
                    Text(
                        nz[nz.size - 1].name,
                        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = p.ink
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            Column {
                rows.forEachIndexed { i, r ->
                    if (i > 0) ItemDivider()
                    val last = lastStudied[r.id]
                    val daysSince = if (last != null) ChronoUnit.DAYS.between(dkey(last), LocalDate.now()) else null
                    val stale = r.real && (last == null || daysSince != null && daysSince >= 7)
                    Column(Modifier.padding(vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(r.color)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(start = 10.dp)
                            ) {
                                Text(
                                    r.name,
                                    style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                                    color = p.ink
                                )
                                if (r.id == most) Tag("Most", false)
                                else if (r.id == least) Tag("Least", true)
                                else if (r.v == 0L && r.real) Tag("Not studied", true)
                            }
                            Mut(
                                hm(r.v) + (if (tt > 0) " · " + Math.round(r.v.toDouble() / tt * 100) + "%" else ""),
                                size = 13.sp
                            )
                        }
                        Bar(r.v.toFloat() / top, height = 6.dp, color = r.color, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
                        val ago = if (!r.real) "" else if (last == null) "never studied"
                        else if ((daysSince ?: 0L) <= 0L) "studied today" else "$daysSince d since last study"
                        if (ago.isNotEmpty() || r.chapters > 0) {
                            Mut(
                                listOfNotNull(
                                    ago.ifEmpty { null },
                                    if (r.chapters > 0 && r.real) "${r.doneCh}/${r.chapters} chapters" else null
                                ).joinToString(" · "),
                                size = 12.sp,
                                color = if (stale && ago.isNotEmpty()) p.red else p.mut,
                                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, low: Boolean) {
    val p = pal()
    val c = if (low) p.red else p.ok
    Text(
        text,
        style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 11.sp),
        color = c,
        modifier = Modifier
            .padding(start = 6.dp)
            .background(lerp(p.card, c, 0.18f), RoundedCornerShape(99.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/* ---------------- today list ---------------- */

@Composable
private fun TodayCard() {
    val p = pal()
    val t0 = sod(System.currentTimeMillis())
    val ts = Model.S.ses.filter { it.t >= t0 }.reversed()
    AppCard {
        H2("What you studied today")
        if (ts.isEmpty()) {
            Mut("Nothing logged today. Start the timer on Home.")
        } else {
            Column {
                ts.forEachIndexed { i, s ->
                    if (i > 0) ItemDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        val subs = s.s.mapNotNull { id -> Model.S.subs.find { it.id == id } }
                        Dot(if (subs.isNotEmpty()) subjectColor(subs[0].c) else p.mut)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                if (subs.isNotEmpty()) subs.joinToString(", ") { it.n } else "General study",
                                style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                                color = p.ink
                            )
                            Mut(fmtClock(s.t - s.d * 1000L) + " · " + hm(s.d.toLong()), size = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- heatmap ---------------- */

@Composable
private fun HeatmapCard() {
    val p = pal()
    val m = Model.dayMap()
    val top = (Model.S.goal.d * 3600).let { if (it > 0) it.toLong() else max(3600L, (m.values.maxOrNull() ?: 0L)) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val weeks = 22
    val d0 = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())
    val scroll = rememberScrollState()
    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        scroll.scrollTo(scroll.maxValue)
    }
    AppCard {
        H2("Study heatmap · last $weeks weeks")
        Column(Modifier.horizontalScroll(scroll)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (w in 0 until weeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (d in 0 until 7) {
                            val date = d0.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val level: Int
                            val visible = !date.isAfter(today)
                            if (!visible) level = -1
                            else {
                                val v = m[date] ?: 0L
                                level = when {
                                    v <= 0 -> 0
                                    v < top * 25 / 100 -> 1
                                    v < top * 50 / 100 -> 2
                                    v < top * 90 / 100 -> 3
                                    else -> 4
                                }
                            }
                            val color = when (level) {
                                0 -> p.trk
                                1 -> lerp(p.trk, p.a, 0.28f)
                                2 -> lerp(p.trk, p.a, 0.5f)
                                3 -> lerp(p.trk, p.a, 0.75f)
                                4 -> p.a
                                else -> Color.Transparent
                            }
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .background(color, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Mut("Less", size = 12.sp)
            listOf(p.trk, lerp(p.trk, p.a, 0.28f), lerp(p.trk, p.a, 0.5f), lerp(p.trk, p.a, 0.75f), p.a).forEach { c ->
                Box(Modifier.size(14.dp).background(c, RoundedCornerShape(3.dp)))
            }
            Mut("More · based on your daily goal", size = 12.sp, modifier = Modifier.padding(start = 3.dp))
        }
    }
}

/* ---------------- best ---------------- */

@Composable
private fun BestCard() {
    val dm = mutableMapOf<Long, Long>()
    val wm = mutableMapOf<Long, Long>()
    Model.S.ses.forEach { s ->
        val d = sod(s.t)
        dm[d] = (dm[d] ?: 0L) + s.d
        val w = wk(s.t)
        wm[w] = (wm[w] ?: 0L) + s.d
    }
    val bd = dm.maxByOrNull { it.value }
    val bw = wm.maxByOrNull { it.value }
    val allTime = Model.S.ses.sumOf { it.d.toLong() }
    val cells = listOf(
        Triple("Best day", if (bd != null) hm(bd.value) else "–", if (bd != null) fmtDate(bd.key) else ""),
        Triple("Best week", if (bw != null) hm(bw.value) else "–", if (bw != null) "Week of " + fmtDate(bw.key) else ""),
        Triple("All-time total", hm(allTime), ""),
        Triple("Days studied", dm.size.toString(), "")
    )
    AppCard {
        val wide = LocalConfiguration.current.screenWidthDp >= 600
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cells.forEach { BestCell(it.first, it.second, it.third, Modifier.weight(1f)) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BestCell(cells[0].first, cells[0].second, cells[0].third, Modifier.weight(1f))
                BestCell(cells[1].first, cells[1].second, cells[1].third, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BestCell(cells[2].first, cells[2].second, cells[2].third, Modifier.weight(1f))
                BestCell(cells[3].first, cells[3].second, cells[3].third, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BestCell(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val p = pal()
    Column(modifier) {
        Mut(label, size = 13.sp)
        Text(
            value,
            style = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
            color = p.ink
        )
        if (sub.isNotEmpty()) Mut(sub, size = 13.sp)
    }
}
