package com.devfahim00.zerostudy

/** A short (1-2 s), loud alert sound bundled in res/raw. */
data class AlertSound(val id: String, val name: String, val hint: String, val res: Int)

object AlertSounds {
    const val DEFAULT = "sonar"

    val all = listOf(
        AlertSound("beep", "Classic beep", "Triple digital beep", R.raw.alert_beep),
        AlertSound("chime", "Chime", "Two bright bell notes", R.raw.alert_chime),
        AlertSound("bell", "Bell", "One big bell strike", R.raw.alert_bell),
        AlertSound("digital", "Digital", "Fast electronic arpeggio", R.raw.alert_digital),
        AlertSound("alarm", "Alarm clock", "Rattling clock bell", R.raw.alert_alarm),
        AlertSound("marimba", "Marimba", "Soft wooden melody", R.raw.alert_marimba),
        AlertSound("sonar", "Sonar", "Ping with an echo", R.raw.alert_sonar),
        AlertSound("success", "Success", "Rising level-up fanfare", R.raw.alert_success),
        AlertSound("siren", "Siren", "Loud wailing siren", R.raw.alert_siren),
        AlertSound("buzzer", "Buzzer", "Harsh low buzzer", R.raw.alert_buzzer),
        AlertSound("coin", "Coin", "Quick arcade coin", R.raw.alert_coin),
        AlertSound("rise", "Whistle rise", "Sweeping rising tone", R.raw.alert_rise)
    )

    fun byId(id: String?): AlertSound = all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT }
}

/** A long, seamless background loop (res/raw) that plays while a focus session runs. */
data class AmbientSound(val id: String, val name: String, val hint: String, val res: Int)

object AmbientSounds {
    val all = listOf(
        AmbientSound("off", "Off", "No background sound", 0),
        AmbientSound("rain", "Rain", "Soft rain with distant drops", R.raw.ambient_rain),
        AmbientSound("lofi", "Lo-fi", "Mellow keys, bass and a slow beat", R.raw.ambient_lofi)
    )

    fun byId(id: String?): AmbientSound = all.firstOrNull { it.id == id } ?: all.first()
}
