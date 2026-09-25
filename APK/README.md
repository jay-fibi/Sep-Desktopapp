# Camera Measure — installable APK

| | |
|---|---|
| File | `CameraMeasure-1.0-debug.apk` |
| Size | 10,666,664 bytes |
| SHA-256 | `a8cc011e0b7f1988f52c9b50b48da1142822eb7e2764ec0a6fc6ec3535dd5f85` |
| Package | `com.example.camerameasure` — versionName `1.0`, versionCode `1` |
| Android | 7.0 (API 24) or newer · targetSdk 34 |
| CPUs | universal: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| Signing | Android **debug** keystore, APK Signature Scheme v2 — sideload only, not for Google Play |

## Install

```bash
# 1) over USB with debugging enabled
adb install -r CameraMeasure-1.0-debug.apk

# 2) manually: copy the APK to the phone (USB, cloud drive, e-mail), tap it in the file
#    manager and allow "Install unknown apps" for that app when asked.
```

On first launch the app asks for the camera permission, then shows the live preview. Tap the
shutter, mark the two ends of a reference object of known size (an A4 sheet, a bank card or a
coin preset work well), enter its real length, and afterwards tap the two ends of anything you
want to measure. Results are shown in mm / cm / m / inch and can be shared as text.

## Verify the download

```bash
sha256sum CameraMeasure-1.0-debug.apk
# a8cc011e0b7f1988f52c9b50b48da1142822eb7e2764ec0a6fc6ec3535dd5f85

apksigner verify --print-certs CameraMeasure-1.0-debug.apk
# Verifies — "CN=Android Debug" certificate

aapt2 dump badging CameraMeasure-1.0-debug.apk
# package: com.example.camerameasure  ·  launchable-activity: .MainActivity
```

## Rebuild it yourself

The full source of the app is in the `CameraMeasure/` folder of this repository:

```bash
cd CameraMeasure
./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and Android SDK platform 34 + build-tools 34.0.0 (both bundled with recent
Android Studio). The app is built with Kotlin 1.9.24, AGP 8.5.2 and Gradle 8.7; 22 JVM unit
tests (`./gradlew testDebugUnitTest`) cover the measurement maths, the unit conversion and the
photo down-sampling.
