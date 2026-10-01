package com.devfahim00.zerostudy

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/* ---------------- data model (mirrors the web app's localStorage schema) ---------------- */

data class Chapter(
    val id: String = "",
    val n: String = "",
    val done: Long = 0,
    val rv: Int = 0,
    val last: Long = 0,
    val dif: String = "",
    val note: String = ""
)

data class Subject(
    val id: String = "",
    val n: String = "",
    val c: String = "",
    val ch: List<Chapter> = emptyList(),
    val wg: Double = 0.0
)

data class Session(
    val t: Long = 0,
    val d: Int = 0,
    val s: List<String> = emptyList(),
    val c: String = ""
)

data class Goal(val d: Double = 2.0, val w: Double = 10.0)

data class Cfg(val f: Int = 25, val b: Int = 5, val rv: List<Int> = listOf(1, 3, 7, 15, 30))

data class TimerState(val run: Boolean = false, val start: Long = 0, val acc: Long = 0)

data class Exam(val n: String = "", val d: String = "")

data class AppState(
    val subs: List<Subject> = emptyList(),
    val ses: List<Session> = emptyList(),
    val goal: Goal = Goal(),
    val sel: List<String> = emptyList(),
    val tm: TimerState = TimerState(),
    val cfg: Cfg = Cfg(),
    val ph: String = "f",
    val open: List<String> = emptyList(),
    val exam: Exam = Exam(),
    val theme: String = "dark",
    val sc: String = ""
)

/* ---------------- model ---------------- */

object Model {

    private const val PREFS = "focus.studytracker.v1"
    val COL = listOf(
        "#7cc4ff", "#a78bfa", "#5eead4", "#fbbf24",
        "#fb7185", "#86efac", "#f0abfc", "#fdba74"
    )

    private val gson = Gson()
    private lateinit var appCtx: Context

    var S by mutableStateOf(AppState())
    var now by mutableStateOf(System.currentTimeMillis())
    var fullscreen by mutableStateOf(false)
    var statsRange by mutableStateOf("d")

    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var player: MediaPlayer? = null

    fun init(ctx: Context) {
        if (::appCtx.isInitialized) return
        appCtx = ctx.applicationContext
        val json = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("state", null)
        S = if (json != null) {
            try {
                fix(gson.fromJson(json, AppState::class.java))
            } catch (e: Exception) {
                AppState()
            }
        } else AppState()
    }

    /** Defensive fill for any missing/null collections from older JSON. */
    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun fix(s: AppState?): AppState {
        if (s == null) return AppState()
        return AppState(
            subs = (s.subs ?: emptyList()).map { sub ->
                sub.copy(ch = (sub.ch ?: emptyList()).map { it.copy() })
            },
            ses = s.ses ?: emptyList(),
            goal = s.goal ?: Goal(),
            sel = s.sel ?: emptyList(),
            tm = s.tm ?: TimerState(),
            cfg = (s.cfg ?: Cfg()).let { it.copy(rv = if (it.rv.isNullOrEmpty()) listOf(1, 3, 7, 15, 30) else it.rv) },
            ph = if (s.ph.isNullOrBlank()) "f" else s.ph,
            open = s.open ?: emptyList(),
            exam = s.exam ?: Exam(),
            theme = if (s.theme.isNullOrBlank()) "dark" else s.theme,
            sc = s.sc ?: ""
        )
    }

    fun save() {
        try {
            appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("state", gson.toJson(S)).apply()
        } catch (_: Exception) {
        }
    }

    fun toast(msg: String) {
        toasts.tryEmit(msg)
    }

    fun beep() {
        try {
            player?.release()
            player = MediaPlayer.create(appCtx, R.raw.beep)?.also { p ->
                p.setOnCompletionListener { it.release() }
                p.start()
            }
        } catch (_: Exception) {
        }
    }

    /* ---------------- timer engine ---------------- */

    fun el(): Long {
        val t = S.tm
        val n = System.currentTimeMillis()
        return t.acc + (if (t.run) n - t.start else 0L)
    }

    fun tgt(): Long = ((if (S.ph == "f") S.cfg.f else S.cfg.b) * 60_000L)

    fun dispSec(): Int {
        val t = tgt()
        val e = el()
        return if (t > 0) Math.ceil(Math.max(0.0, (t - e) / 1000.0)).toInt()
        else Math.floor(e / 1000.0).toInt()
    }

    fun tick() {
        now = System.currentTimeMillis()
        advance()
    }

    private fun advance() {
        val t = tgt()
        val e = el()
        if (S.ph == "f") {
            if (t == 0L || e < t) return
        } else {
            if (e < t) return
        }
        val over = e - t
        val nowMs = System.currentTimeMillis()
        if (S.ph == "f") {
            S = S.copy(ses = S.ses + Session(t = nowMs - over, d = (t / 1000).toInt(), s = S.sel.toList(), c = effSc()))
            if (S.cfg.b > 0) {
                S = S.copy(ph = "b", tm = TimerState(run = true, start = nowMs - over, acc = 0))
                toast("Session done. Break started")
            } else {
                resetTm()
                toast("Session done")
            }
        } else {
            S = S.copy(ph = "f")
            resetTm()
            toast("Break over. Ready for the next session")
        }
        save()
        beep()
        advance()
    }

    private fun resetTm() {
        S = S.copy(tm = TimerState())
    }

    fun toggle() {
        val t = S.tm
        S = if (t.run) {
            S.copy(tm = t.copy(acc = t.acc + (System.currentTimeMillis() - t.start), run = false))
        } else {
            S.copy(tm = t.copy(start = System.currentTimeMillis(), run = true))
        }
        save()
    }

    fun stop() {
        if (S.ph == "b") {
            S = S.copy(ph = "f")
            resetTm()
            save()
            return
        }
        val e = el()
        if (e < 5000) {
            toast("Study at least 5 seconds to save")
            return
        }
        S = S.copy(
            ses = S.ses + Session(
                t = System.currentTimeMillis(),
                d = Math.round(e / 1000.0).toInt(),
                s = S.sel.toList(),
                c = effSc()
            )
        )
        resetTm()
        save()
        toast("Saved " + hm(e / 1000))
    }

    /** Chapter id is only valid when exactly one subject with chapters is selected. */
    fun effSc(): String {
        if (S.sel.size == 1) {
            val sub = S.subs.find { it.id == S.sel[0] }
            if (sub != null && sub.ch.any { it.id == S.sc }) return S.sc
        }
        return ""
    }

    fun liveFocusSec(): Long = if (S.ph == "f") el() / 1000 else 0L

    /* ---------------- derived helpers ---------------- */

    fun dayMap(): Map<LocalDate, Long> {
        val m = mutableMapOf<LocalDate, Long>()
        S.ses.forEach { s -> val k = dkey(s.t); m[k] = (m[k] ?: 0L) + s.d }
        return m
    }

    fun todaySec(): Long {
        val today = dkey(now)
        return (dayMap()[today] ?: 0L) + liveFocusSec()
    }

    fun weekSec(): Long {
        val w0 = wk(now)
        var wt = liveFocusSec()
        S.ses.forEach { s -> if (wk(s.t) == w0) wt += s.d }
        return wt
    }

    fun streak(): Int {
        val m = dayMap().toMutableMap()
        val today = dkey(now)
        m[today] = (m[today] ?: 0L) + liveFocusSec()
        var d = today
        if ((m[d] ?: 0L) <= 0L) d = d.minusDays(1)
        var n = 0
        while ((m[d] ?: 0L) > 0L) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /* ---------------- revisions ---------------- */

    fun dueOf(c: Chapter): Long? {
        if (c.done <= 0 || c.rv >= S.cfg.rv.size) return null
        var d = addDays(c.done, S.cfg.rv[c.rv].toLong())
        if (c.last > 0) d = maxOf(d, addDays(c.last, 1))
        return d
    }

    fun whenStr(d: Long): String {
        val n = dayDiff(d, System.currentTimeMillis())
        return when {
            n < 0 -> "${-n}d overdue"
            n == 0L -> "due today"
            n == 1L -> "tomorrow"
            else -> "in $n days"
        }
    }

    fun chStatus(c: Chapter): String {
        if (c.done <= 0) return "Not completed"
        val d = dueOf(c)
        return "Completed " + fmtDate(c.done) + " · " + when {
            d == null -> "Mastered"
            d <= System.currentTimeMillis() -> "Revision due"
            else -> "Next revision " + fmtDate(d)
        }
    }

    data class DueItem(val sub: Subject, val ch: Chapter, val due: Long)

    fun dueItems(): List<DueItem> {
        val all = mutableListOf<DueItem>()
        S.subs.forEach { s -> s.ch.forEach { c -> dueOf(c)?.let { all.add(DueItem(s, c, it)) } } }
        all.sortBy { it.due }
        return all
    }

    fun revise(subId: String, chId: String, ok: Boolean) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val ci = S.subs[si].ch.indexOfFirst { it.id == chId }
        if (ci < 0) return
        val c = S.subs[si].ch[ci]
        val nowMs = System.currentTimeMillis()
        val nc = if (ok) c.copy(rv = c.rv + 1, last = nowMs) else c.copy(rv = 0, done = nowMs, last = 0)
        val nchapters = S.subs[si].ch.toMutableList().also { it[ci] = nc }
        S = S.copy(subs = S.subs.toMutableList().also { it[si] = S.subs[si].copy(ch = nchapters) })
        toast(
            if (ok) {
                if (nc.rv >= S.cfg.rv.size) "Chapter mastered" else "Next revision " + whenStr(dueOf(nc) ?: nowMs)
            } else "Cycle restarted · " + whenStr(dueOf(nc) ?: nowMs)
        )
        save()
    }

    /* ---------------- subjects & chapters ---------------- */

    fun addSubject(name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        if (S.subs.any { it.n.equals(n, ignoreCase = true) }) {
            toast("Subject already exists")
            return false
        }
        S = S.copy(
            subs = S.subs + Subject(
                id = "s" + System.currentTimeMillis().toString(36),
                n = n,
                c = COL[S.subs.size % COL.size]
            )
        )
        save()
        toast("Added $n")
        return true
    }

    fun removeSubject(id: String) {
        S = S.copy(
            subs = S.subs.filter { it.id != id },
            sel = S.sel - id,
            open = S.open - id
        )
        save()
    }

    fun addChapter(subId: String, name: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return false
        val s = S.subs[si]
        if (s.ch.any { it.n.equals(n, ignoreCase = true) }) {
            toast("Chapter already exists")
            return false
        }
        val nc = Chapter(id = "c" + System.currentTimeMillis().toString(36) + (0..999).random(), n = n)
        val nsub = s.copy(ch = s.ch + nc)
        S = S.copy(
            subs = S.subs.toMutableList().also { it[si] = nsub },
            open = if (S.open.contains(subId)) S.open else S.open + subId
        )
        save()
        return true
    }

    fun removeChapter(subId: String, chId: String) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        S = S.copy(subs = S.subs.toMutableList().also { it[si] = S.subs[si].copy(ch = S.subs[si].ch.filter { it.id != chId }) })
        save()
    }

    fun toggleChapterDone(subId: String, chId: String) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val ci = S.subs[si].ch.indexOfFirst { it.id == chId }
        if (ci < 0) return
        val c = S.subs[si].ch[ci]
        val nc = if (c.done > 0) {
            c.copy(done = 0, rv = 0, last = 0)
        } else {
            c.copy(done = System.currentTimeMillis(), rv = 0, last = 0)
        }
        if (c.done > 0) toast("Unmarked") else toast("Chapter complete · first revision " + whenStr(dueOf(nc) ?: 0L))
        S = S.copy(subs = S.subs.toMutableList().also { sub ->
            sub[si] = sub[si].copy(ch = sub[si].ch.toMutableList().also { it[ci] = nc })
        })
        save()
    }

    fun setDiff(subId: String, chId: String, k: String) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val ci = S.subs[si].ch.indexOfFirst { it.id == chId }
        if (ci < 0) return
        val c = S.subs[si].ch[ci]
        val nc = c.copy(dif = if (c.dif == k) "" else k)
        S = S.copy(subs = S.subs.toMutableList().also { sub ->
            sub[si] = sub[si].copy(ch = sub[si].ch.toMutableList().also { it[ci] = nc })
        })
        save()
    }

    fun setNote(subId: String, chId: String, note: String) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val ci = S.subs[si].ch.indexOfFirst { it.id == chId }
        if (ci < 0) return
        val c = S.subs[si].ch[ci]
        S = S.copy(subs = S.subs.toMutableList().also { sub ->
            sub[si] = sub[si].copy(ch = sub[si].ch.toMutableList().also { it[ci] = c.copy(note = note.trim()) })
        })
        save()
        toast("Note saved")
    }

    fun setWeeklyGoal(subId: String, hours: Double) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        S = S.copy(subs = S.subs.toMutableList().also { it[si] = it[si].copy(wg = Math.max(0.0, hours)) })
        save()
    }

    fun setOpen(id: String, open: Boolean) {
        S = S.copy(
            open = if (open) (if (S.open.contains(id)) S.open else S.open + id)
            else S.open - id
        )
        save()
    }

    fun toggleSel(id: String?) {
        S = if (id == null) S.copy(sel = emptyList())
        else if (S.sel.contains(id)) S.copy(sel = S.sel - id)
        else S.copy(sel = S.sel + id)
        save()
    }

    fun setSc(id: String) {
        S = S.copy(sc = id)
        save()
    }

    /* ---------------- settings ---------------- */

    fun setPreset(f: Int, b: Int) {
        S = S.copy(cfg = S.cfg.copy(f = f, b = b))
        save()
    }

    fun setSessionMin(f: Int) {
        S = S.copy(cfg = S.cfg.copy(f = f.coerceIn(0, 600)))
        save()
    }

    fun setBreakMin(b: Int) {
        S = S.copy(cfg = S.cfg.copy(b = b.coerceIn(0, 120)))
        save()
    }

    fun setDailyGoal(h: Double) {
        S = S.copy(goal = S.goal.copy(d = Math.max(0.0, h)))
        save()
    }

    fun setWeeklyGoal(h: Double) {
        S = S.copy(goal = S.goal.copy(w = Math.max(0.0, h)))
        save()
    }

    fun setExamName(n: String) {
        S = S.copy(exam = S.exam.copy(n = n.take(30)))
        save()
    }

    fun setExamDate(d: String) {
        S = S.copy(exam = S.exam.copy(d = d))
        save()
    }

    fun setTheme(t: String) {
        S = S.copy(theme = t)
        save()
    }

    fun setRevSchedule(text: String): Boolean {
        val a = text.split(Regex("[^0-9]+"))
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 1..365 }
            .distinct()
            .sorted()
            .take(8)
        if (a.isEmpty()) return false
        S = S.copy(cfg = S.cfg.copy(rv = a))
        save()
        toast("Schedule updated")
        return true
    }

    /** Days until the exam, or null when no valid future date is set. */
    fun examDays(): Long? {
        val d = S.exam.d
        if (d.isBlank()) return null
        return try {
            val date = LocalDate.parse(d)
            ChronoUnit.DAYS.between(LocalDate.now(), date)
        } catch (e: Exception) {
            null
        }
    }
}
