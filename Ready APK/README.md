# Ready APK

**JacksPingPong.apk** — ready-to-install debug build of *Jacks PingPong* (by Jack).

- **App**: Jacks PingPong
- **Package**: `com.example.pingpong`
- **Version**: 1.0 (versionCode 1)
- **minSdk**: 26 (Android 8.0) · **targetSdk**: 34 (Android 14)
- **Build type**: debug (debug-signed, installable directly)

## Install

```bash
adb install JacksPingPong.apk
```

Or copy the APK to the device and open it (allow "install from unknown
sources" when prompted).

Built from source in `PingPongGame/` with `./gradlew assembleDebug`;
all 12 unit tests passed.
