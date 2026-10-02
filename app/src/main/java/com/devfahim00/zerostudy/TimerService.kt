package com.devfahim00.zerostudy

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the study timer alive in the background.
 *
 * - Shows an ongoing notification with a live countdown (the system draws the chronometer,
 *   so it stays accurate without waking the app) and a Pause / Resume button.
 * - Keeps the process alive so the end-of-session alert can play while the app is closed.
 * - Schedules an exact alarm for the end of the phase, so the alert also fires on time when
 *   the phone is idle (Doze) or the screen is off.
 */
class TimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false
    private var lastSig = ""
    private val islandSupported by lazy { HyperIsland.supported(this) }
    private val island: Boolean get() = islandSupported && Model.S.isl != "off"

    private val ticker = object : Runnable {
        override fun run() {
            Model.tick()
            if (!active()) {
                shutdown()
                return
            }
            if (signature() != lastSig) refresh()
            else if (island && Model.S.tm.run && foreground) {
                // the island shows text, so it needs a fresh value every second
                try {
                    NotificationManagerCompat.from(this@TimerService).notify(NOTIF_RUN, buildNotification())
                } catch (_: SecurityException) {
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Model.init(applicationContext)
        instance = this
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Model.init(applicationContext)
        if (intent?.action == ACT_TOGGLE) Model.toggle()
        // a notification must be posted right away after startForegroundService
        if (!active()) {
            // started without anything to show (e.g. timer was reset meanwhile)
            startForegroundCompat(buildNotification())
            shutdown()
            return START_NOT_STICKY
        }
        refresh()
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, 1000)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Re-posts the notification and (re)schedules the end-of-phase alarm. */
    fun refresh() {
        if (!active()) {
            shutdown()
            return
        }
        lastSig = signature()
        val n = buildNotification()
        if (!foreground) startForegroundCompat(n)
        else NotificationManagerCompat.from(this).let {
            try {
                it.notify(NOTIF_RUN, n)
            } catch (_: SecurityException) {
            }
        }
        scheduleAlarm(this)
    }

    fun shutdown() {
        handler.removeCallbacks(ticker)
        cancelAlarm(this)
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        foreground = false
        stopSelf()
    }

    private fun startForegroundCompat(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_RUN, n, type)
            foreground = true
        } catch (_: Exception) {
            // not allowed to start from here: the alarm + in-app ticker still handle the alert
        }
    }

    private fun signature(): String {
        val t = Model.S.tm
        return "${Model.S.ph}|${t.run}|${t.start}|${t.acc}|${Model.tgt()}|${subjectLabel()}|${Model.S.isl}"
    }

    private fun subjectLabel(): String {
        val names = Model.S.subs.filter { Model.S.sel.contains(it.id) }.map { it.n }
        return when {
            names.isEmpty() -> ""
            names.size == 1 -> names[0]
            else -> "${names.size} subjects"
        }
    }

    private fun buildNotification(): Notification {
        val t = Model.S.tm
        val target = Model.tgt()
        val focus = Model.S.ph == "f"
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggle = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).setAction(ACT_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val subject = subjectLabel()
        val title = if (focus) (if (subject.isNotEmpty()) "Focus · $subject" else "Focus session") else "Break"
        val b = NotificationCompat.Builder(this, CH_RUN)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (island) b.addExtras(HyperIsland.picsExtras(this))
        if (t.run) {
            if (target > 0) {
                val endAt = t.start + (target - t.acc)
                b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(endAt)
                b.setContentText("Ends at ${fmtClock(endAt)}")
            } else {
                b.setUsesChronometer(true).setWhen(t.start - t.acc)
                b.setContentText("Stopwatch running")
            }
            b.addAction(0, "Pause", toggle)
        } else {
            val left = if (target > 0) hm(maxOf(0L, (target - t.acc) / 1000)) + " left" else hm(t.acc / 1000)
            b.setShowWhen(false).setContentText("Paused · $left")
            b.addAction(0, "Resume", toggle)
        }
        val n = b.build()
        if (island) {
            val sec = Model.dispSec().toLong()
            val time = if (sec >= 3600) "${sec / 3600}:${p2((sec % 3600) / 60)}:${p2(sec % 60)}" else "${p2(sec / 60)}:${p2(sec % 60)}"
            val status = when {
                !t.run -> "Paused"
                target <= 0L -> "Stopwatch"
                else -> "left"
            }
            val label = (if (focus) "Focus" else "Break") + (if (focus && subject.isNotEmpty()) " · $subject" else "")
            HyperIsland.attach(n, label, time, status, Model.S.isl)
        }
        return n
    }

    companion object {
        const val CH_RUN = "timer_running"
        const val CH_DONE = "timer_done"
        const val NOTIF_RUN = 1001
        const val NOTIF_DONE = 1002
        const val ACT_TOGGLE = "com.devfahim00.zerostudy.TOGGLE"

        @Volatile
        var instance: TimerService? = null

        /** A timer is "active" while it runs or is paused part-way. */
        fun active(): Boolean = Model.S.tm.run || Model.S.tm.acc > 0L

        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            val inst = instance
            if (active()) {
                if (inst != null) inst.refresh()
                else try {
                    ContextCompat.startForegroundService(app, Intent(app, TimerService::class.java))
                } catch (_: Exception) {
                }
            } else {
                if (inst != null) inst.shutdown() else cancelAlarm(app)
            }
        }

        private fun createChannels(ctx: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH_RUN, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Live countdown while a focus session or break is running"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_DONE, "Session finished", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Shown when a focus session or break ends (the alert sound is played by the app)"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }

        /** "Session done" heads-up, only when the app is not on screen. */
        fun notifyDone(ctx: Context, title: String, text: String) {
            if (Model.appVisible) return
            try {
                createChannels(ctx)
                val open = PendingIntent.getActivity(
                    ctx, 2,
                    Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                val n = NotificationCompat.Builder(ctx, CH_DONE)
                    .setSmallIcon(R.drawable.ic_stat_timer)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setSilent(true)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(open)
                    .build()
                NotificationManagerCompat.from(ctx).notify(NOTIF_DONE, n)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
            ctx, 3,
            Intent(ctx, TimerAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun scheduleAlarm(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val t = Model.S.tm
            val target = Model.tgt()
            if (!t.run || target <= 0L) {
                am.cancel(alarmIntent(ctx))
                return
            }
            val endAt = t.start + (target - t.acc)
            val pi = alarmIntent(ctx)
            try {
                if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
                } else {
                    val show = PendingIntent.getActivity(
                        ctx, 4, Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(endAt, show), pi)
                }
            } catch (_: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi)
            }
        }

        fun cancelAlarm(ctx: Context) {
            try {
                (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(alarmIntent(ctx))
            } catch (_: Exception) {
            }
        }
    }
}

/** Fires at the exact end of a focus session / break, even when the phone is idle. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Model.init(context.applicationContext)
        Model.tick()          // advances the phase, saves, plays the alert and posts "done"
        Model.syncService()   // refresh the notification / schedule the next alarm
    }
}
