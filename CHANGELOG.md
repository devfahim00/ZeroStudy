# Changelog

## [1.3.1] - 2026-10-03

### Changed
- **Cleaner home screen.** The crowded row of pills at the top (streak, goal, level, sound and every exam) is gone. The top now has one slim line: streak and level on the left, the sound switch on the right. Your daily focus goal, level progress and upcoming exams moved into a calm "Today" card below the timer, so nothing is lost but the top stays light.

### Fixed
- **Ambient sound now loops seamlessly.** When Rain or Lo-fi reached the end and started again there was a small pause and the rain had an audible jump. The loop now repeats directly in the audio hardware with no gap, and the Rain loop was smoothed so its end blends into its start.

## [1.3.0] - 2026-10-03

### New
- **Plan tab (Today's plan).** A new page with your checklist for the day. Add tasks (optionally tagged with a subject), tick them off, and see them move to the bottom when done. It also shows today's focus time against your goal, the revisions that are due today (with Done / Forgot buttons) and your level. Unfinished tasks from earlier days show up under "Carried over" and count for today when you tick them.
- **XP and levels.** You earn 1 XP for every minute of focus, 20 XP for completing a chapter (once per chapter), and 10 XP for every revision you mark Done, plus 15 XP extra when a chapter is fully mastered. Your level and progress show on the home screen and the Plan tab, and a short level-up animation plays when you reach a new level. XP for everything you already did in earlier versions is added automatically the first time you open this version.
- **Ambient sound while you focus.** Choose **Rain** or **Lo-fi** in Settings → Ambient sound. The loop plays only while a focus session is running, pauses with the timer and stops for breaks, even with the app in the background. It is stored in the app, so it works offline. The sound pill on the home screen switches Off / Rain / Lo-fi in one tap.
- **Silent update check.** Every time you open the app it quietly checks GitHub for a newer release. Nothing is shown when you are up to date or offline. When a new version exists, a small dialog offers to download it.

### Changed
- The bottom bar has a sixth tab (Plan), so the labels are slightly smaller.

## [1.2.2] - 2026-10-02

### Fixed
- **Hyper Island is now balanced.** The compact island had everything on one side and left the other side empty. It now shows only the app icon on the left and the timer on the right, so both sides of the camera are used evenly.

### Changed
- Removed the "Icon only" island style. Hyper Island settings now have **Compact** and **Off**. If you had Icon only selected it switches to Compact.

## [1.2.1] - 2026-10-02

### Fixed
- **Hyper Island is no longer too wide.** The island used to show a label, the time and a status text plus a second icon, which pushed status bar icons out of view. It is now a small pill with the icon and only the time.

### New
- **Hyper Island settings (Xiaomi / HyperOS).** Settings → Hyper Island lets you pick the style: **Compact** (icon + time), **Icon only** (narrowest) or **Off** (normal timer notification only). The same page shows whether Focus / Island notifications are allowed and opens the notification settings.
- The full details (focus or break, subject, time left) are still shown when you open the island or pull down the notification.

## [1.2.0] - 2026-10-02

### New
- **Backup & restore.** Settings → Backup & restore lets you export all your data (subjects, chapters, revisions, sessions, goals, exams, settings) to a file and import it back, for example on a new phone. Importing shows what is inside the backup and asks before replacing your current data.
- **Hyper Island timer (Xiaomi / HyperOS).** The running timer is now sent as a HyperOS island / focus notification, so the countdown shows around the camera cutout and on the lock screen like the Clock app. Settings has a "Hyper Island timer" row that shows whether it is allowed and opens the notification settings where you turn it on.

### Improved
- The timer notification now also updates every second on HyperOS so the island always shows the current time.

### Notes
- On HyperOS, open **Settings → Hyper Island timer** once and allow "Focus / Island notifications" for ZeroStudy. Without that switch HyperOS only shows the normal notification.

## [1.1.0] - 2026-10-02

### New
- **Timer keeps running in the background.** While a focus session or break is running, a notification shows a live countdown (or the stopwatch) with a **Pause / Resume** button. Tap it to jump back into the app.
- **Alert plays even when the app is closed or the phone is locked.** The end-of-session alarm is scheduled as an exact alarm, so your chosen sound and vibration fire on time, and a "Session done" notification appears.
- **Revisions tab (new).** One screen with your whole revision plan, grouped by **date → subject → chapter**. Today's card has Done / Forgot buttons and shows what is waiting in the queue; you can filter by subject and see how full each day is (for example 3/5).

### Fixed
- **Daily revision limit is now respected everywhere.** Completing a chapter, marking a revision Done or tapping Forgot used to ignore your "max revisions per day" and could pile too many chapters on one day. They now go to the next day that has room.
- Days that were already overfilled are repaired automatically, and changing the daily limit or the revision schedule re-spreads upcoming days.

### Notes
- Android 13 and newer asks for notification permission the first time you open the app. Allow it to see the timer in the notification shade.

## [1.0.1] - 2026-10-02

### Fixed
- **Alert sounds no longer distort.** All 12 sounds were remastered with a smooth limiter instead of hard clipping, so they stay clean while still being loud and equally loud with each other.

### Changed
- **Sonar is now the default alert sound** (you can still pick any of the 12 in Settings → Alert sound).
- **Subject selection on Home is now a dropdown**, like the chapter selector. It supports picking several subjects, and your picks show in a row you can swipe left/right. No more crowded home screen when you have many subjects.

## [1.0.0] - 2026-10-02

First stable release of **ZeroStudy** — a focus & study tracker for Android.

### What you get

**Focus timer**
- Pomodoro-style sessions with presets 25/5, 50/10, 90/15, or a free stopwatch
- Fine-tune focus and break length with a slider or exact -/+ buttons
- Gradient progress ring with ambient particle animation
- Full-screen landscape flip clock while you study

**Alert sounds (new)**
- 12 short (1-2 sec), loud alert sounds: Classic beep, Chime, Bell, Digital, Alarm clock, Marimba, Sonar, Success, Siren, Buzzer, Coin and Whistle rise
- Pick your favourite in **Settings → Alert sound** and preview each one with a tap
- Plays on the alarm volume so it still rings in silent mode, together with a strong vibration

**Subjects & revisions**
- Subjects and chapters with completion tracking, difficulty tags (Easy / Medium / Hard) and notes
- Spaced-repetition revisions on a schedule you can edit (default 1, 3, 7, 15, 30 days) with a daily revision cap
- Backlog chapters are spread out automatically so revisions never pile up
- Lock the subject during a running session

**Stats & goals**
- Daily, weekly and monthly charts, subject comparison and today's sessions
- 22-week study heatmap, best day/week records and streaks
- Daily and weekly study goals with live progress

**Exam countdown**
- Add multiple exams and see the nearest countdown on the home screen

**App & updates (new)**
- **Check for update** in Settings — looks at the GitHub releases and offers the new APK
- **Join Telegram** button in Settings to reach the community
- Dark and light themes
- Everything stays on your device — no account needed
