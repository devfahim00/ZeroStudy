package com.devfahim00.zerostudy

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    val note: String = "",
    val nx: Long = 0,        // earliest allowed next revision (used to spread backlog chapters)
    val bk: Boolean = false, // marked as "studied earlier" when set up
    val xg: Boolean = false  // XP for completing this chapter was already given (no farming by toggling)
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

data class Cfg(
    val f: Int = 25,
    val b: Int = 5,
    val rv: List<Int> = listOf(1, 3, 7, 15, 30),
    val cap: Int = 5 // max revisions per day; <= 0 (old saves) falls back to 5
)

data class TimerState(val run: Boolean = false, val start: Long = 0, val acc: Long = 0)

data class Exam(val id: String = "", val n: String = "", val d: String = "")

/** One task in Today's plan. [d] is the ISO date it was planned for, [sub] an optional subject id. */
data class PlanItem(
    val id: String = "",
    val t: String = "",
    val sub: String = "",
    val d: String = "",
    val done: Boolean = false
)

data class AppState(
    val subs: List<Subject> = emptyList(),
    val ses: List<Session> = emptyList(),
    val goal: Goal = Goal(),
    val sel: List<String> = emptyList(),
    val tm: TimerState = TimerState(),
    val cfg: Cfg = Cfg(),
    val ph: String = "f",
    val open: List<String> = emptyList(),
    val exam: Exam = Exam(), // legacy single exam, migrated into [exams]
    val exams: List<Exam> = emptyList(),
    val theme: String = "dark",
    val sc: String = "",
    val snd: String = "sonar", // id of the chosen alert sound (see AlertSounds)
    val isl: String = "compact", // Hyper Island style: compact | off
    val xp: Long = 0, // total XP, the level is derived from it
    val amb: String = "off", // ambient sound while focusing: off | rain | lofi
    val plan: List<PlanItem> = emptyList()
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

    /** True while the activity is on screen (used to skip the \"session done\" notification). */
    @Volatile var appVisible = false

    /** Starts / refreshes / stops the foreground timer notification to match the timer state. */
    fun syncService() {
        if (!::appCtx.isInitialized) return
        syncAmbient()
        try {
            TimerService.sync(appCtx)
        } catch (_: Exception) {
        }
    }

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
        migrateDefaultSound()
        migrateXp()
        prunePlan()
        rebalanceFuture()
    }

    /** v1.0.0 stored "beep" as the default sound; the default is now "sonar". One-time switch. */
    private fun migrateDefaultSound() {
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("snd_default_sonar", false)) return
        if (S.snd == "beep") S = S.copy(snd = AlertSounds.DEFAULT)
        prefs.edit().putBoolean("snd_default_sonar", true).apply()
        save()
    }

    /** Defensive fill for any missing/null collections from older JSON. */
    @Suppress("UNNECESSARY_SAFE_CALL")
    private fun fix(s: AppState?): AppState {
        if (s == null) return AppState()
        val legacy = s.exam ?: Exam()
        val loaded = (s.exams ?: emptyList()).mapIndexed { i, e ->
            if (e.id.isNullOrBlank()) e.copy(id = "e" + System.currentTimeMillis().toString(36) + i) else e
        }
        val migrated = if (loaded.isEmpty() && !legacy.d.isNullOrBlank()) {
            listOf(Exam(id = "e" + System.currentTimeMillis().toString(36), n = legacy.n ?: "", d = legacy.d))
        } else loaded
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
            exam = Exam(),
            exams = migrated,
            theme = if (s.theme.isNullOrBlank()) "dark" else s.theme,
            sc = s.sc ?: "",
            snd = AlertSounds.byId(s.snd).id,
            isl = if (s.isl == "off") "off" else "compact", // the old "icon" style was removed
            xp = if (s.xp < 0L) 0L else s.xp,
            amb = if (s.amb == "rain" || s.amb == "lofi") s.amb else "off",
            plan = s.plan ?: emptyList()
        )
    }

    /* ---------------- backup: export / import ---------------- */

    private const val BACKUP_VERSION = 1

    /** The whole app state as a JSON backup string. */
    fun exportJson(): String {
        val o = com.google.gson.JsonObject()
        o.addProperty("app", "ZeroStudy")
        o.addProperty("backup", BACKUP_VERSION)
        o.addProperty("exported", System.currentTimeMillis())
        o.add("state", gson.toJsonTree(S))
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(o)
    }

    data class BackupInfo(val state: AppState, val subjects: Int, val chapters: Int, val sessions: Int)

    /** Parses and validates a backup without applying it. Returns null when it is not a ZeroStudy backup. */
    fun parseBackup(text: String): BackupInfo? {
        return try {
            val root = com.google.gson.JsonParser.parseString(text).asJsonObject
            val stateJson = when {
                root.has("state") && root.get("state").isJsonObject -> root.getAsJsonObject("state")
                root.has("subs") -> root // a raw state dump
                else -> return null
            }
            if (!stateJson.has("subs") && !stateJson.has("ses")) return null
            val st = fix(gson.fromJson(stateJson, AppState::class.java))
            BackupInfo(st, st.subs.size, st.subs.sumOf { it.ch.size }, st.ses.size)
        } catch (e: Exception) {
            null
        }
    }

    /** Replaces all data with the backup. The running timer is stopped so nothing stale carries over. */
    fun applyBackup(info: BackupInfo) {
        S = info.state.copy(tm = TimerState(), ph = "f")
        save()
        rebalanceFuture()
        syncService()
        toast("Backup restored")
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

    /**
     * Loud completion alert: plays the sound chosen in Settings on the alarm stream
     * (so it is not tied to the media volume and still rings in silent mode) at full
     * player volume, plus a strong vibration pattern.
     */
    fun beep() {
        playSound(S.snd)
        vibrate()
    }

    /** Plays one alert sound (used by the alert and by the preview in Settings). */
    fun playSound(id: String) {
        try {
            player?.release()
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            player = MediaPlayer.create(appCtx, AlertSounds.byId(id).res, attrs, AudioManager.AUDIO_SESSION_ID_GENERATE)
                ?.also { p ->
                    p.setVolume(1f, 1f)
                    p.setOnCompletionListener { it.release() }
                    p.start()
                }
        } catch (_: Exception) {
        }
    }

    fun setIslandStyle(style: String) {
        S = S.copy(isl = style)
        save()
        syncService()
    }

    fun setSound(id: String) {
        S = S.copy(snd = id)
        save()
        playSound(id)
    }

    private fun vibrate() {
        try {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 700)
            val effect = VibrationEffect.createWaveform(pattern, -1)
            if (Build.VERSION.SDK_INT >= 31) {
                val vm = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val v = appCtx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(effect)
            }
        } catch (_: Exception) {
        }
    }

    /* ---------------- timer engine ---------------- */

    fun el(): Long {
        val t = S.tm
        // Reading `now` subscribes any composable that calls this to the 250 ms
        // ticker, so the countdown actually redraws while the timer runs.
        val n = maxOf(System.currentTimeMillis(), now)
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
        val wasFocus = S.ph == "f"
        if (S.ph == "f") {
            S = S.copy(ses = S.ses + Session(t = nowMs - over, d = (t / 1000).toInt(), s = S.sel.toList(), c = effSc()))
            val gained = focusXp(t / 1000)
            awardXp(gained)
            if (S.cfg.b > 0) {
                S = S.copy(ph = "b", tm = TimerState(run = true, start = nowMs - over, acc = 0))
                toast("Session done. Break started" + xpTag(gained))
            } else {
                resetTm()
                toast("Session done" + xpTag(gained))
            }
        } else {
            S = S.copy(ph = "f")
            resetTm()
            toast("Break over. Ready for the next session")
        }
        save()
        beep()
        TimerService.notifyDone(
            appCtx,
            if (wasFocus) "Session done" else "Break over",
            if (!wasFocus) "Ready for the next session" else if (S.ph == "b") "Break started" else "Nice work"
        )
        syncService()
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
        syncService()
    }

    fun stop() {
        if (S.ph == "b") {
            S = S.copy(ph = "f")
            resetTm()
            save()
            syncService()
            return
        }
        val e = el()
        if (e < 5000) {
            // too short to count: reset, which also unlocks the subject selection
            resetTm()
            save()
            toast("Under 5 seconds, not saved. Timer reset")
            syncService()
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
        val gained = focusXp(Math.round(e / 1000.0))
        awardXp(gained)
        resetTm()
        save()
        syncService()
        toast("Saved " + hm(e / 1000) + xpTag(gained))
    }

    /** Subject/chapter can't be changed once a focus session has started (running or paused). */
    fun subjectLocked(): Boolean = S.ph == "f" && (S.tm.run || S.tm.acc > 0L)

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
        if (c.nx > 0) d = maxOf(d, c.nx)
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
        return (if (c.bk) "Studied earlier" else "Completed " + fmtDate(c.done)) + " · " + when {
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

    /** Max revisions we ask for in one day. */
    fun revCap(): Int = if (S.cfg.cap <= 0) 5 else S.cfg.cap

    /** Everything due now or earlier, most overdue first. */
    fun dueNow(): List<DueItem> {
        val n = System.currentTimeMillis()
        return dueItems().filter { it.due <= n }
    }

    /** What the badge/list shows: the daily cap, the rest wait in the queue. */
    fun dueShownCount(): Int = minOf(dueNow().size, revCap())

    /** How many revisions are already planned for each day from today (overdue counts as today). */
    private fun loadByDay(excludeChapter: String? = null): MutableMap<Long, Int> {
        val m = mutableMapOf<Long, Int>()
        val n = System.currentTimeMillis()
        dueItems().forEach {
            if (it.ch.id == excludeChapter) return@forEach
            val k = maxOf(0L, dayDiff(it.due, n))
            m[k] = (m[k] ?: 0) + 1
        }
        return m
    }

    /** Earliest day >= [minDay] that still has room under the daily cap. Reserves that slot. */
    private fun placeDay(load: MutableMap<Long, Int>, minDay: Long): Long {
        val cap = revCap()
        var day = maxOf(1L, minDay)
        while ((load[day] ?: 0) >= cap) day++
        load[day] = (load[day] ?: 0) + 1
        return day
    }

    /** Earliest day (>= the first interval) that still has room under the daily cap. */
    private fun takeSlot(load: MutableMap<Long, Int>): Long = placeDay(load, S.cfg.rv.first().toLong())

    /**
     * Makes sure no future day holds more than [revCap] revisions. Extra chapters move to
     * the next day that has room. Runs on start-up and whenever the cap or schedule changes,
     * which also repairs days that were overfilled by older versions.
     */
    fun rebalanceFuture() {
        val cap = revCap()
        val nowMs = System.currentTimeMillis()
        val items = dueItems().filter { dayDiff(it.due, nowMs) >= 1 }
        if (items.isEmpty()) return
        val byDay = java.util.TreeMap<Long, MutableList<DueItem>>()
        items.forEach { byDay.getOrPut(dayDiff(it.due, nowMs)) { mutableListOf() }.add(it) }
        val moved = mutableMapOf<String, Long>()
        var d = byDay.firstKey()
        while (d <= byDay.lastKey()) {
            val list = byDay[d]
            if (list != null && list.size > cap) {
                val extra = list.subList(cap, list.size).toList()
                while (list.size > cap) list.removeAt(list.size - 1)
                val next = byDay.getOrPut(d + 1) { mutableListOf() }
                extra.forEach { moved[it.ch.id] = addDays(nowMs, d + 1); next.add(it) }
            }
            d++
        }
        if (moved.isEmpty()) return
        S = S.copy(subs = S.subs.map { sub ->
            sub.copy(ch = sub.ch.map { c -> moved[c.id]?.let { c.copy(nx = it) } ?: c })
        })
        save()
    }

    /**
     * Marks chapters as already studied before using the app. They count as completed
     * but their first revisions are spread across the coming days (at most [revCap]
     * per day) instead of all landing on day one.
     */
    fun markStudiedEarlier(subId: String, chIds: List<String>) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val load = loadByDay()
        val nowMs = System.currentTimeMillis()
        var count = 0
        var lastDay = 0L
        val chapters = S.subs[si].ch.map { c ->
            if (c.id in chIds && c.done <= 0) {
                val day = takeSlot(load)
                count++
                lastDay = maxOf(lastDay, day)
                c.copy(done = nowMs, rv = 0, last = 0, nx = addDays(nowMs, day), bk = true)
            } else c
        }
        if (count == 0) return
        S = S.copy(subs = S.subs.toMutableList().also { it[si] = it[si].copy(ch = chapters) })
        save()
        toast(
            if (count == 1) "Marked as studied · first revision " + whenStr(addDays(nowMs, lastDay))
            else "$count chapters marked · revisions spread over $lastDay days"
        )
    }

    fun markAllStudiedEarlier(subId: String) {
        val sub = S.subs.find { it.id == subId } ?: return
        markStudiedEarlier(subId, sub.ch.filter { it.done <= 0 }.map { it.id })
    }

    fun setRevCap(n: Int) {
        S = S.copy(cfg = S.cfg.copy(cap = n.coerceIn(1, 50)))
        save()
        rebalanceFuture()
    }

    fun revise(subId: String, chId: String, ok: Boolean) {
        val si = S.subs.indexOfFirst { it.id == subId }
        if (si < 0) return
        val ci = S.subs[si].ch.indexOfFirst { it.id == chId }
        if (ci < 0) return
        val c = S.subs[si].ch[ci]
        val nowMs = System.currentTimeMillis()
        val load = loadByDay(excludeChapter = chId)
        val nc = if (ok) {
            val nrv = c.rv + 1
            if (nrv >= S.cfg.rv.size) {
                c.copy(rv = nrv, last = nowMs, nx = 0)
            } else {
                val natural = maxOf(addDays(c.done, S.cfg.rv[nrv].toLong()), addDays(nowMs, 1))
                val day = placeDay(load, dayDiff(natural, nowMs))
                c.copy(rv = nrv, last = nowMs, nx = addDays(nowMs, day))
            }
        } else {
            val day = placeDay(load, S.cfg.rv.first().toLong())
            c.copy(rv = 0, done = nowMs, last = 0, nx = addDays(nowMs, day), bk = false)
        }
        val nchapters = S.subs[si].ch.toMutableList().also { it[ci] = nc }
        S = S.copy(subs = S.subs.toMutableList().also { it[si] = S.subs[si].copy(ch = nchapters) })
        val gain = if (!ok) 0L else if (nc.rv >= S.cfg.rv.size) XP_REVISION + XP_MASTER else XP_REVISION
        awardXp(gain)
        toast(
            if (ok) {
                if (nc.rv >= S.cfg.rv.size) "Chapter mastered" + xpTag(gain) else "Next revision " + whenStr(dueOf(nc) ?: nowMs) + xpTag(gain)
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
        if (subjectLocked() && S.sel.contains(id)) {
            toast("This subject is in use. Save or reset the session first")
            return
        }
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
            c.copy(done = 0, rv = 0, last = 0, nx = 0, bk = false)
        } else {
            val nowMs = System.currentTimeMillis()
            val day = placeDay(loadByDay(excludeChapter = chId), S.cfg.rv.first().toLong())
            c.copy(done = nowMs, rv = 0, last = 0, nx = addDays(nowMs, day), bk = false, xg = true)
        }
        val gain = if (c.done <= 0 && !c.xg) XP_CHAPTER else 0L
        awardXp(gain)
        if (c.done > 0) toast("Unmarked") else toast("Chapter complete · first revision " + whenStr(dueOf(nc) ?: 0L) + xpTag(gain))
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
        if (subjectLocked()) {
            toast("Subject is locked during a session")
            return
        }
        S = if (id == null) S.copy(sel = emptyList())
        else if (S.sel.contains(id)) S.copy(sel = S.sel - id)
        else S.copy(sel = S.sel + id)
        save()
    }

    fun setSc(id: String) {
        if (subjectLocked()) {
            toast("Chapter is locked during a session")
            return
        }
        S = S.copy(sc = id)
        save()
    }

    /* ---------------- settings ---------------- */

    fun setPreset(f: Int, b: Int) {
        S = S.copy(cfg = S.cfg.copy(f = f, b = b))
        save()
        syncService()
    }

    fun setSessionMin(f: Int) {
        S = S.copy(cfg = S.cfg.copy(f = f.coerceIn(0, 600)))
        save()
        syncService()
    }

    fun setBreakMin(b: Int) {
        S = S.copy(cfg = S.cfg.copy(b = b.coerceIn(0, 120)))
        save()
        syncService()
    }

    fun setDailyGoal(h: Double) {
        S = S.copy(goal = S.goal.copy(d = Math.max(0.0, h)))
        save()
    }

    fun setWeeklyGoal(h: Double) {
        S = S.copy(goal = S.goal.copy(w = Math.max(0.0, h)))
        save()
    }

    fun addExam(name: String, date: String): Boolean {
        if (date.isBlank()) {
            toast("Pick an exam date")
            return false
        }
        val n = name.trim().take(30).ifBlank { "Exam" }
        S = S.copy(
            exams = S.exams + Exam(
                id = "e" + System.currentTimeMillis().toString(36) + (0..999).random(),
                n = n,
                d = date
            )
        )
        save()
        toast("Added $n")
        return true
    }

    fun removeExam(id: String) {
        S = S.copy(exams = S.exams.filter { it.id != id })
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
        rebalanceFuture()
        toast("Schedule updated")
        return true
    }

    /** Days from today until [d] (ISO date), or null when it can't be parsed. */
    fun daysUntil(d: String): Long? {
        if (d.isBlank()) return null
        return try {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(d))
        } catch (e: Exception) {
            null
        }
    }

    /** Exams that have not passed yet, nearest first, paired with days left. */
    fun upcomingExams(): List<Pair<Exam, Long>> =
        S.exams.mapNotNull { e -> daysUntil(e.d)?.takeIf { it >= 0 }?.let { e to it } }
            .sortedBy { it.second }

    /* ---------------- XP & levels ---------------- */

    private const val XP_CHAPTER = 20L   // completing a chapter (once per chapter)
    private const val XP_REVISION = 10L  // a revision marked Done
    private const val XP_MASTER = 15L    // extra for finishing the last revision of a chapter
    private const val XP_STEP = 60.0     // level n starts at XP_STEP * (n - 1)^2

    /** Set to the new level when the player levels up; the UI shows a short animation and clears it. */
    var levelUp by mutableStateOf<Int?>(null)
        private set

    fun dismissLevelUp() {
        levelUp = null
    }

    fun levelOf(xp: Long): Int = Math.sqrt(xp.coerceAtLeast(0L) / XP_STEP).toInt() + 1

    fun levelStartXp(level: Int): Long = (XP_STEP * (level - 1) * (level - 1)).toLong()

    /** XP earned inside the current level (first) and the XP the whole level needs (second). */
    fun levelProgress(): Pair<Long, Long> {
        val lv = levelOf(S.xp)
        val start = levelStartXp(lv)
        return (S.xp - start) to (levelStartXp(lv + 1) - start)
    }

    /** 1 XP per full minute of focus. */
    private fun focusXp(sec: Long): Long = sec / 60

    private fun xpTag(n: Long): String = if (n > 0) " · +$n XP" else ""

    /** Adds XP (the caller saves). Starts the level-up animation when a new level is reached. */
    private fun awardXp(n: Long) {
        if (n <= 0L) return
        val before = levelOf(S.xp)
        S = S.copy(xp = S.xp + n)
        val after = levelOf(S.xp)
        if (after > before) {
            levelUp = after
            playSound("success")
        }
    }

    /** One-time: gives XP for everything already done before XP existed (v1.3.0). */
    private fun migrateXp() {
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("xp_backfilled", false)) return
        val minutes = S.ses.sumOf { it.d.toLong() / 60L }
        val chapters = S.subs.sumOf { sub ->
            sub.ch.sumOf { c ->
                (if (c.done > 0 && !c.bk) XP_CHAPTER else 0L) + c.rv.toLong() * XP_REVISION
            }
        }
        S = S.copy(
            xp = S.xp + minutes + chapters,
            subs = S.subs.map { sub -> sub.copy(ch = sub.ch.map { it.copy(xg = it.xg || (it.done > 0 && !it.bk)) }) }
        )
        prefs.edit().putBoolean("xp_backfilled", true).apply()
        save()
    }

    /* ---------------- ambient sound ---------------- */

    private const val AMB_VOLUME = 0.6f

    private var ambientPlayer: MediaPlayer? = null
    private var ambientId: String? = null
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val endPreview = Runnable {
        if (ambientId?.startsWith("preview:") == true) {
            stopAmbient()
            syncAmbient()
        }
    }

    /** Plays the chosen loop while a focus session is running; stops on pause, break or stop. */
    fun syncAmbient() {
        if (!::appCtx.isInitialized) return
        val want = if (S.amb != "off" && S.ph == "f" && S.tm.run) S.amb else null
        if (want == ambientId) return
        stopAmbient()
        if (want != null) startAmbient(want)
    }

    private fun startAmbient(id: String) {
        val res = AmbientSounds.byId(id).res
        if (res == 0) return
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val mp = MediaPlayer.create(appCtx, res, attrs, AudioManager.AUDIO_SESSION_ID_GENERATE) ?: return
            mp.isLooping = true
            mp.setVolume(AMB_VOLUME, AMB_VOLUME)
            mp.start()
            ambientPlayer = mp
            ambientId = id
        } catch (_: Exception) {
            stopAmbient()
        }
    }

    private fun stopAmbient() {
        mainHandler.removeCallbacks(endPreview)
        try {
            ambientPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            ambientPlayer?.release()
        } catch (_: Exception) {
        }
        ambientPlayer = null
        ambientId = null
    }

    /** Plays a few seconds of a loop so it can be heard in Settings. */
    fun previewAmbient(id: String) {
        stopAmbient()
        if (id == "off") {
            syncAmbient()
            return
        }
        startAmbient(id)
        if (ambientPlayer != null) {
            ambientId = "preview:$id"
            mainHandler.postDelayed(endPreview, 5000)
        }
    }

    fun setAmbient(id: String) {
        S = S.copy(amb = id)
        save()
        previewAmbient(id)
    }

    /** Quick toggle on the home screen: off -> rain -> lo-fi -> off. */
    fun cycleAmbient() {
        val order = listOf("off", "rain", "lofi")
        val next = order[(order.indexOf(S.amb) + 1) % order.size]
        S = S.copy(amb = next)
        save()
        syncAmbient()
        toast(if (next == "off") "Ambient sound off" else "Ambient: " + AmbientSounds.byId(next).name + " · plays while you focus")
    }

    /* ---------------- today's plan ---------------- */

    private fun todayIso(): String = dkey(System.currentTimeMillis()).toString()

    fun planToday(): List<PlanItem> {
        val t = todayIso()
        return S.plan.filter { it.d == t }
    }

    /** Unfinished tasks from earlier days. */
    fun planCarried(): List<PlanItem> {
        val t = todayIso()
        return S.plan.filter { it.d < t && !it.done }
    }

    fun addPlan(text: String, subId: String): Boolean {
        val t = text.trim().take(80)
        if (t.isEmpty()) return false
        if (planToday().any { it.t.equals(t, ignoreCase = true) }) {
            toast("Already in today's plan")
            return false
        }
        S = S.copy(
            plan = S.plan + PlanItem(
                id = "p" + System.currentTimeMillis().toString(36) + (0..999).random(),
                t = t,
                sub = subId,
                d = todayIso()
            )
        )
        save()
        return true
    }

    /** Ticks / unticks a task. Ticking an old unfinished task moves it into today. */
    fun togglePlan(id: String) {
        val t = todayIso()
        S = S.copy(plan = S.plan.map { if (it.id == id) it.copy(done = !it.done, d = t) else it })
        save()
    }

    fun removePlan(id: String) {
        S = S.copy(plan = S.plan.filter { it.id != id })
        save()
    }

    /** Drops finished tasks after 2 days and unfinished ones after 2 weeks. */
    private fun prunePlan() {
        val today = dkey(System.currentTimeMillis())
        val keep = S.plan.filter {
            val d = try {
                LocalDate.parse(it.d)
            } catch (e: Exception) {
                return@filter false
            }
            val age = ChronoUnit.DAYS.between(d, today)
            if (it.done) age <= 2 else age <= 14
        }
        if (keep.size != S.plan.size) {
            S = S.copy(plan = keep)
            save()
        }
    }
}
