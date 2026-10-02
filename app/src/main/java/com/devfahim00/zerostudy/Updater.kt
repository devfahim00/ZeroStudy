package com.devfahim00.zerostudy

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL

/** Checks the GitHub releases of this app for a newer version. */
object Updater {

    private const val API = "https://api.github.com/repos/devfahim00/ZeroStudy/releases/latest"
    const val RELEASES_PAGE = "https://github.com/devfahim00/ZeroStudy/releases"

    sealed class State {
        object Idle : State()
        object Checking : State()
        object UpToDate : State()
        data class Available(val version: String, val apkUrl: String?, val page: String, val notes: String) : State()
        data class Failed(val reason: String) : State()
    }

    var state: State by mutableStateOf(State.Idle)
        private set

    fun currentVersion(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    fun check(ctx: Context) {
        if (state is State.Checking) return
        state = State.Checking
        val current = currentVersion(ctx)
        Thread {
            state = try {
                val conn = URL(API).openConnection() as HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "ZeroStudy-Android")
                when (val code = conn.responseCode) {
                    200 -> {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val o = JsonParser.parseString(body).asJsonObject
                        val tag = o.get("tag_name").asString
                        if (isNewer(tag, current)) {
                            var apk: String? = null
                            o.getAsJsonArray("assets")?.forEach { a ->
                                val ao = a.asJsonObject
                                if (apk == null && ao.get("name").asString.endsWith(".apk")) {
                                    apk = ao.get("browser_download_url").asString
                                }
                            }
                            State.Available(
                                version = tag.trimStart('v', 'V'),
                                apkUrl = apk,
                                page = o.get("html_url").asString,
                                notes = o.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""
                            )
                        } else State.UpToDate
                    }
                    404 -> State.UpToDate // no release published yet
                    else -> State.Failed("GitHub returned $code")
                }
            } catch (e: Exception) {
                State.Failed("No internet connection")
            }
        }.start()
    }

    fun dismiss() {
        state = State.Idle
    }

    /** Opens the APK download (or the release page when there is no APK asset). */
    fun open(ctx: Context, url: String) = openUrl(ctx, url)

    fun openUrl(ctx: Context, url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Model.toast("No app found to open the link")
        }
    }

    private fun parts(v: String): List<Int> =
        v.trim().trimStart('v', 'V').substringBefore('-').split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

    /** Semantic-version compare: is [tag] strictly newer than [current]? */
    fun isNewer(tag: String, current: String): Boolean {
        val a = parts(tag)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
