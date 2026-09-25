# Camera Measure

An Android app that turns your phone camera into a ruler: take a photo that contains an object
of known size, calibrate against it, and read real-world distances straight off the picture.

* **Kotlin + Jetpack Compose (Material 3)** UI, single-activity, no Java sources.
* **CameraX** for the live preview and the still capture (`camera-core`, `camera-camera2`,
  `camera-lifecycle`, `camera-view`).
* **Reference-scale measurement**: pure geometry, no AR/ML dependencies, works on any device
  with a camera (minSdk 24).

## How the measurement works

```
mm_per_pixel = reference_length_mm / reference_distance_px     (calibration)
measured_mm  = distance_px * mm_per_pixel
```

1. **Calibrate** – tap the two ends of an object whose length you know (A4 sheet 297 mm, bank
   card 85.6 mm, a coin, … or type your own value). The app shows the resulting scale in mm/px.
2. **Measure** – tap the two ends of any distance on the photo. Every measurement is added to a
   list, colour-coded on the photo, and can be switched between mm / cm / m / inch.
3. **Share** – the top bar sends the calibration and all results as plain text.

Distances are computed in the coordinate space of the decoded photo, so the result is
independent of the current zoom level, the preview resolution and the screen size.

### Limitations (important)

A photo is a perspective projection, so this method is exact only when the reference object and
the measured object **lie in the same plane, parallel to the camera sensor**. Tilt or depth
difference between the two introduces an error that grows with the angle. The app is therefore a
good fit for measuring objects on a table/floor or on a flat sheet of paper, and a poor fit for
distances that recede into the picture. Everything is computed in 2D – for perspective-correct,
"walk around the room" measuring you would need ARCore depth (a possible future extension).

Accuracy also depends on how precisely the two end points are tapped; use pinch-zoom (up to 8x)
before placing the points, the marked position is the centre of the crosshair/ring.

## Project layout

```
CameraMeasure/
├── app/src/main/java/com/example/camerameasure/
│   ├── MainActivity.kt              # single activity, hosts the Compose tree
│   ├── camera/CameraSession.kt      # CameraX preview + ImageCapture, torch, JPEG output
│   ├── camera/PhotoDecoder.kt       # EXIF-aware, memory-friendly bitmap decoding
│   ├── measure/MeasureMath.kt       # pure geometry: distances, scale, angles
│   ├── measure/Units.kt             # mm/cm/m/inch conversion and formatting
│   ├── model/Models.kt              # NormPoint, Calibration, Measurement, presets
│   ├── ui/CameraMeasureApp.kt       # permission gate + camera/measure navigation
│   ├── ui/CameraScreen.kt           # preview, framing grid, shutter, torch
│   ├── ui/MeasureScreen.kt          # controls, result list, reference dialog, sharing
│   ├── ui/PhotoMeasurementCanvas.kt # photo + overlay rendering and tap/zoom gestures
│   ├── ui/MeasureViewModel.kt       # session state (photo, calibration, measurements)
│   └── ui/theme/                    # Material 3 colours and typography
└── app/src/test/java/...            # JVM unit tests (22 tests)
```

## Build & run

```bash
# Debug APK
./gradlew assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk

# Unit tests
./gradlew testDebugUnitTest      # report: app/build/reports/tests/testDebugUnitTest/index.html

# Install on a connected device
./gradlew installDebug
```

Requirements: JDK 17, Android SDK platform 34 + build-tools 34.0.0 (AGP 8.5.2 / Gradle 8.7).
`local.properties` must point at your SDK (`sdk.dir=/path/to/Android/Sdk`) or `ANDROID_HOME` has
to be set. The project also opens directly in Android Studio (Ladybug or newer).

Permissions: `android.permission.CAMERA` only. Photos are written to the app cache
(`cacheDir/captures`), never to shared storage, and nothing leaves the device unless you press
share.

## Install on a phone (prebuilt APK)

A ready-to-install debug build is published in the `APK/` folder of this repository:

```
APK/CameraMeasure-1.0-debug.apk
  10,666,664 bytes · SHA-256 a8cc011e0b7f1988f52c9b50b48da1142822eb7e2764ec0a6fc6ec3535dd5f85
```

It is a debug-signed, universal APK (arm64-v8a, armeabi-v7a, x86, x86_64) that runs on
Android 7.0 (API 24) and newer — see `APK/README.md` for the install and verification steps.
Rebuilding from this source produces the same app:

```bash
./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
```

It is a **universal, debug-signed APK** (arm64-v8a, armeabi-v7a, x86, x86_64), so it installs on
any phone or tablet running Android 7.0 (API 24) or newer. Verify it with:

```bash
apksigner verify --print-certs CameraMeasure-1.0-debug.apk   # shows the Android Debug certificate
aapt2 dump badging CameraMeasure-1.0-debug.apk               # package, launcher activity, ABIs
```

Options to get it onto the device:

```bash
# 1) via USB debugging
adb install -r CameraMeasure-1.0-debug.apk

# 2) manually: copy the APK to the phone (USB, cloud drive, e-mail), tap it in the
#    file manager and allow "Install unknown apps" for that app when asked.
```

Because it is signed with the Android debug keystore it cannot be published on Google Play and
cannot be upgraded by a release-signed build — use your own keystore (or Android Studio →
*Build → Generate Signed App Bundle / APK*) for a store build. Rebuilding from source always
produces the same APK:

```bash
./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
```

## Tests

`MeasureMathTest` covers the calibration/measurement maths with real-world numbers
(a 4032x3024 photo, an A4 reference), `UnitsTest` the unit conversion/formatting and
`PhotoDecoderTest` the down-sampling logic that keeps 12 MP photos within memory limits.

## Possible next steps

* Persist the measurement sessions (Room) so a project can be reopened.
* ARCore Depth API for perspective-correct measurement of non-planar scenes.
* A movable/editable end point (drag instead of delete + re-tap) and a magnifier loupe.
* Perspective/rectification mode: pick the four corners of a plane to remove the tilt error.
