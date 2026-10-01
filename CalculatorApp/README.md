# Theme Calculator — Android App

A modern calculator for Android with **five selectable color themes**, written in Java.
The theme is switched at runtime from inside the app and remembered across launches.

## Features

- **Basic arithmetic** — `+`, `−`, `×`, `÷` with correct operator precedence
  (`2+3×4` = 14, not 20), decimal input, `%`, sign toggle (`±`), backspace and clear
- **Live result preview** while you type, plus a small history line after `=`
- **Five themes** — Light, Dark, Ocean, Forest, Sunset — switchable via the palette
  button in the top bar; the choice is persisted in `SharedPreferences`
- **Sensible edge handling** — division-by-zero shows *Error* instead of crashing,
  binary-float noise is trimmed (`0.1+0.2` = `0.3`), leading operators are replaced,
  and very large/small results fall back to scientific notation
- **Rotation-safe** — the expression, result and history survive configuration changes
- Ripple touch feedback, auto-sizing result text, theme-aware status/navigation bars

## Themes

| Theme  | Style                       |
|--------|-----------------------------|
| Light  | Clean whites, orange accent (default) |
| Dark   | Charcoal greys, orange accent         |
| Ocean  | Deep navy, cyan accent                |
| Forest | Dark green, lime accent               |
| Sunset | Deep plum, pink accent                |

All five are regular Android themes (`res/values/themes.xml`) that fill a set of
custom theme attributes (`res/values/attrs.xml`). A single layout resolves colors
through `?attr/...`, so adding a sixth theme is just a new `<style>` block plus an
enum entry in `ThemeManager.AppTheme` — no layout or Java changes needed.

## Project structure

```
CalculatorApp/
├── app/src/main/
│   ├── java/com/example/calculator/
│   │   ├── MainActivity.java        # UI wiring, theme picker dialog, state save/restore
│   │   ├── CalculatorEngine.java    # Pure-Java calculation logic (no Android deps)
│   │   └── ThemeManager.java        # Theme enum + SharedPreferences persistence
│   ├── res/
│   │   ├── layout/activity_main.xml # Display + 5×4 keypad
│   │   ├── values/                  # attrs, colors, themes, styles, strings, dimens
│   │   ├── drawable/                # Ripple button backgrounds, icons
│   │   └── mipmap-anydpi-v26/       # Adaptive launcher icon
│   └── AndroidManifest.xml
└── app/src/test/...                 # 31 JUnit tests for CalculatorEngine
```

`CalculatorEngine` is deliberately Android-free: it keeps the typed expression,
evaluates it with a shunting-yard parser (respecting precedence and unary minus)
and is fully unit-tested on the JVM.

## Build & run

Requires Android Studio (Hedgehog+) or a JDK 17 + Android SDK 34 setup.

```bash
# Open the CalculatorApp folder in Android Studio, or from the command line:
./gradlew assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # runs the 31 JVM unit tests
./gradlew installDebug           # installs on a connected device/emulator
```

- `compileSdk 34`, `minSdk 26`, `targetSdk 34`
- Android Gradle Plugin 8.2.2, Gradle 8.5 (wrapper included)
- Dependencies: AppCompat 1.6.1, Material 1.11.0, JUnit 4.13.2 (test)

## Verification performed

- ✅ `./gradlew assembleDebug` — APK builds cleanly
- ✅ `./gradlew testDebugUnitTest` — 31/31 tests pass
- Engine additionally compiled and tested standalone with `javac`/`JUnitCore`
