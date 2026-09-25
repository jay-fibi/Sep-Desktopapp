# Jacks PingPong for Android 🏓

A classic ping pong (table tennis) game for Android, written in **Kotlin**
and branded **by Jack** — royal navy & gold identity with the signature
gold "J" monogram launcher icon. Play against an AI opponent: drag your
finger to move the paddle, angle your returns, and be the first to score
5 points.

## Branding

- 🎨 **Jack brand palette** — royal navy `#14264C` + gold `#FFC107`
- 🥇 **Gold "J" monogram** adaptive launcher icon with paddle & ball
- 🏅 **Branded start screen** — gold badge with "J", "by Jack" byline

## Features

- 🎮 **Smooth 60 fps gameplay** — vsync-driven game loop via `Choreographer`
- 👆 **Touch controls** — drag anywhere to move your paddle horizontally
- 🤖 **AI opponent** with 3 difficulty levels (Easy / Medium / Hard) that
  predicts the ball trajectory, including wall bounces
- 🏓 **Realistic physics** — return angle depends on where the ball hits the
  paddle; ball speed increases during long rallies
- 🔊 **Retro sound effects** — generated programmatically (no audio assets):
  paddle hits, wall bounces, scoring
- 🏆 **Match scoring** — first to 5 points wins; live score drawn on the table
- ⏸️ **Pause/resume** integrated with the activity lifecycle
- 📱 **Immersive fullscreen** portrait mode, screen kept on while playing

## Project structure

```
PingPongGame/
├── app/
│   ├── build.gradle.kts          # App module configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/pingpong/
│       │   ├── MainActivity.kt   # Lifecycle, immersive mode, settings dialog
│       │   ├── GameView.kt       # Game loop, physics, AI, rendering, branding
│       │   ├── Ball.kt           # Ball entity: velocity, bounce physics
│       │   ├── Paddle.kt         # Paddle entity: movement + collision
│       │   └── SoundManager.kt   # Procedurally generated sound effects
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/           # strings, colors, themes
│           └── mipmap-*/         # adaptive launcher icon (Jack "J" monogram)
├── build.gradle.kts              # Top-level build file
├── settings.gradle.kts
└── gradle.properties
```

## How to play

1. **Tap** the screen to start the match.
2. **Drag** left/right to move the gold paddle (bottom).
3. Hit the ball past the red AI paddle (top) to score.
4. The spot where the ball hits your paddle controls the return angle.
5. First to **5 points** wins. Tap to play again.

## Building

Requires Android Studio (Hedgehog or newer) or a JDK 17 + Android SDK 34
setup:

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
A prebuilt APK is also available in the `Ready APK/` directory at the
repository root.

## Requirements

- **minSdk**: 26 (Android 8.0)
- **targetSdk / compileSdk**: 34 (Android 14)
- Kotlin 1.9, AGP 8.2

## Implementation notes

- **Game loop**: `Choreographer.FrameCallback` ties updates to the display
  refresh; `dt` is clamped to 33 ms so backgrounding the app never causes
  physics jumps.
- **Collision**: circle-vs-rect closest-point test for the paddles; the ball
  is nudged out of the paddle before reflecting to avoid double-collision.
- **AI**: linear trajectory prediction with side-wall reflection, plus a
  per-difficulty aim error and speed cap so it stays beatable.
- **Sound**: tones are synthesized as 16-bit PCM into a static `AudioTrack`
  on a worker thread — zero asset size, and audio glitches can never crash
  the game loop.
