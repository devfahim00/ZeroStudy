# ZeroStudy 📚⏱️

A beautiful focus & study tracker app for Android, built with Kotlin + Jetpack Compose.
Native port of the *Focus – Study Tracker* web app — same UI, same features.

## Features

- **Focus timer** — Pomodoro-style focus/break sessions (25/5, 50/10, 90/15 or free stopwatch) with a gradient progress ring and ambient particle animation
- **Full-screen flip clock** — big animated flip digits while you study
- **Subjects & chapters** — track what you study, mark chapters complete, set difficulty (Easy/Medium/Hard) and notes
- **Spaced-repetition revisions** — chapters come back for revision on a configurable schedule (default 1, 3, 7, 15, 30 days); *Done* pushes the next revision out, *Forgot* restarts the cycle
- **Stats** — daily / weekly / monthly bar charts, subject comparison, today's sessions, a 22-week study heatmap, best day/week records and streaks
- **Goals** — daily & weekly study goals with live progress
- **Exam countdown** — name a date and get a countdown on the home screen
- **Dark & light themes**
- Everything is stored locally on your device — no account, no network.

## Download the APK

Every push to this repo is built by GitHub Actions. Grab the latest debug APK from the
[**debug-latest release**](https://github.com/devfahim00/ZeroStudy/releases/tag/debug-latest),
or from the **Actions → latest run → Artifacts** section.

> The debug APK is signed with the debug key — allow "Install unknown apps" when prompted.

## Build it yourself

```bash
git clone https://github.com/devfahim00/ZeroStudy.git
cd ZeroStudy
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17. Android SDK with platform 34.

## Tech stack

- Kotlin, Jetpack Compose (Material 3)
- MVVM-style observable state model + Gson/SharedPreferences persistence
- Custom Canvas animations (timer ring, particle dust, flip clock)
- GitHub Actions CI with automatic APK release

## License

App code: MIT. The bundled [Sora](https://fonts.google.com/specimen/Sora) font is licensed under the SIL Open Font License 1.1.
