package com.devfahim00.zerostudy

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Xiaomi HyperOS "Super Island" / Focus notification support.
 *
 * The island is a normal notification that carries extra JSON in the `miui.focus.param`
 * extra. Phones that are not HyperOS simply ignore it, so it is safe to always attach.
 * The user has to allow Focus / Island notifications for the app once (see [openSettings]).
 */
object HyperIsland {

    private const val PIC_ICON = "miui.focus.pic_icon"

    /** 0 = no focus notifications, 2 = HyperOS 2 (status bar + shade), 3 = HyperOS 3 (island). */
    fun protocol(ctx: Context): Int = try {
        Settings.System.getInt(ctx.contentResolver, "notification_focus_protocol", 0)
    } catch (_: Exception) {
        0
    }

    fun supported(ctx: Context): Boolean = protocol(ctx) >= 2

    /** Whether the user allowed Focus / Island notifications for this app. Slow call, run off the main thread. */
    suspend fun hasPermission(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("content://miui.statusbar.notification.public")
            val extras = Bundle().apply { putString("package", ctx.packageName) }
            ctx.contentResolver.call(uri, "canShowFocus", null, extras)?.getBoolean("canShowFocus", false) ?: false
        } catch (_: Exception) {
            false
        }
    }

    /** Opens this app's notification settings, where the Focus / Island switch lives. */
    fun openSettings(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            Model.toast("Open Settings → Notifications → ZeroStudy")
        }
    }

    /** Extra bundle with the island icon; add it to the builder before build(). */
    fun picsExtras(ctx: Context): Bundle {
        val pics = Bundle()
        val icon = Icon.createWithResource(ctx, R.drawable.ic_stat_timer)
        pics.putParcelable(PIC_ICON, icon)
        return Bundle().apply { putBundle("miui.focus.pics", pics) }
    }

    /**
     * Attaches the island JSON to a built notification.
     *
     * [style] "compact": small pill with the icon and only the time, so the status bar keeps
     * its room. "icon": just the icon in the pill (time is still in the expanded card).
     */
    fun attach(n: Notification, title: String, time: String, status: String, style: String) {
        try {
            val pic = JSONObject().put("type", 1).put("pic", PIC_ICON)
            val left = JSONObject().put("type", 1).put("picInfo", pic)
            if (style == "compact") {
                // only the time: no label or sub text, which is what made the island wide
                left.put("textInfo", JSONObject().put("title", time).put("useHighLight", false))
            }
            val island = JSONObject()
                .put("islandProperty", 1)
                .put("bigIslandArea", JSONObject().put("imageTextInfoLeft", left))
                .put("smallIslandArea", JSONObject().put("picInfo", pic))
            val v2 = JSONObject()
                .put("protocol", 1)
                .put("business", "study_timer")
                .put("enableFloat", false)
                .put("islandFirstFloat", false)
                .put("updatable", true)
                .put("ticker", time)
                .put("tickerPic", PIC_ICON)
                .put("aodTitle", "$title  $time")
                .put("aodPic", PIC_ICON)
                .put("param_island", island)
                .put(
                    "baseInfo",
                    JSONObject().put("title", time).put("content", "$title · $status").put("type", 2)
                )
            n.extras.putString("miui.focus.param", JSONObject().put("param_v2", v2).toString())
        } catch (_: Exception) {
        }
    }
}
