package com.devfahim00.zerostudy

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.devfahim00.zerostudy.ui.FullscreenTimer
import com.devfahim00.zerostudy.ui.HomeScreen
import com.devfahim00.zerostudy.ui.SettingsScreen
import com.devfahim00.zerostudy.ui.StatsScreen
import com.devfahim00.zerostudy.ui.SubjectsScreen
import com.devfahim00.zerostudy.ui.ZeroStudyTheme
import com.devfahim00.zerostudy.ui.pal
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Model.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            ZeroStudyTheme {
                App()
            }
        }
    }
}

private data class NavItem(val id: String, val label: String, val icon: ImageVector)

@Composable
fun App() {
    val p = pal()
    var tab by rememberSaveable { mutableStateOf("home") }
    val snackbar = remember { SnackbarHostState() }

    // 250 ms ticker, like the web app's setInterval(tick, 250)
    LaunchedEffect(Unit) {
        while (true) {
            Model.tick()
            delay(250)
        }
    }
    // toast channel -> snackbar
    LaunchedEffect(Unit) {
        Model.toasts.collect { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = Model.S.theme == "light"
            controller.isAppearanceLightNavigationBars = Model.S.theme == "light"
        }
    }

    // immersive mode while the fullscreen clock is open
    LaunchedEffect(Model.fullscreen) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        if (Model.fullscreen) {
            // landscape clock that follows the sensor (flips between both landscape sides)
            window.attributes = window.attributes.also {
                if (Build.VERSION.SDK_INT >= 28) {
                    it.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            (view.context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            (view.context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val items = listOf(
        NavItem("home", "Home", Icons.Rounded.Home),
        NavItem("stats", "Stats", Icons.Rounded.BarChart),
        NavItem("subjects", "Subjects", Icons.Rounded.MenuBook),
        NavItem("settings", "Settings", Icons.Rounded.Tune)
    )

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Scaffold(
            containerColor = p.bg,
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = p.ink,
                        contentColor = p.bg
                    )
                }
            },
            bottomBar = {
                BottomNav(items, tab) { tab = it }
            }
        ) { pad ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                key(tab) {
                    when (tab) {
                        "home" -> HomeScreen()
                        "stats" -> StatsScreen()
                        "subjects" -> SubjectsScreen()
                        "settings" -> SettingsScreen()
                    }
                }
            }
        }
        if (Model.fullscreen) {
            FullscreenTimer()
        }
    }
}

@Composable
private fun BottomNav(items: List<NavItem>, current: String, onTab: (String) -> Unit) {
    val p = pal()
    val dueCount = Model.dueShownCount()
    Column(
        Modifier
            .fillMaxWidth()
            .background(p.nav)
            .drawBehind {
                drawLine(
                    color = p.line,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            items.forEach { item ->
                val on = item.id == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .background(if (on) p.card else androidx.compose.ui.graphics.Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .clickable { onTab(item.id) }
                        .padding(vertical = 12.dp)
                ) {
                    Box {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (on) p.ink else p.mut,
                            modifier = Modifier.size(22.dp)
                        )
                        if (item.id == "home" && dueCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .background(p.red, androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dueCount.toString(),
                                    style = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                                    color = androidx.compose.ui.graphics.Color(0xFF05060A)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        style = TextStyle(fontFamily = com.devfahim00.zerostudy.ui.Sora, fontWeight = FontWeight.Normal, fontSize = 11.sp),
                        color = if (on) p.ink else p.mut
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .width(28.dp)
                            .height(2.dp)
                            .background(if (on) p.a else androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
