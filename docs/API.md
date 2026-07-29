# API Reference — `miui.lights.ILightsManager`

> Updated from live decompilation of `miui-services.jar` (`HyperLightsService`)
> and `framework.jar` (`ILightsManager`, `android.app.ColorLightManager`) pulled
> from a POCO X8 Pro (klee), HyperOS 3 / Android 16 (SDK 36).

The interface descriptor is exactly the string:

```
miui.lights.ILightsManager
```

## Confirmed method table (transaction codes 1–4)

These four transaction codes were independently confirmed twice: once by
manual decompilation of the AIDL stub, and once by blind Binder-transaction
fuzzing (a separate tool that brute-forced codes 1–20 found exactly 4
non-throwing void methods on codes 1–4, with no return data — matching this
table exactly).

| Code | Method | Signature |
|------|--------|-----------|
| 1 | `setColorfulLight` | `(String pkg, int styleType, int userId)` |
| 2 | `setColorCommon` | `(int color, String pkg, int styleType, int userId)` |
| 3 | `setColorLed` | `(int color, String pkg, int styleType, int userId, int category)` |
| 4 | `setCustomLight` | `(int color, int flashMode, int onMs, int offMs, int brightNessMode, String pkg, int styleType, int userId)` |

**Only code 4 (`setCustomLight`) is confirmed to actually drive the camera
ring hardware.** Codes 1–3 return successfully (no exception) but are
effectively no-ops on this HyperOS build for the camera ring:

- `setColorLed` (code 3) — the server-side implementation in
  `HyperLightsService` is an **empty method body**. Always returns
  successfully, never touches hardware.
- `setColorCommon` (code 2) — only has an effect when `styleType == 3`
  (the "music rhythm" LED strip), and only sets a solid color with no
  blink control. Any other `styleType` is a silent no-op.
- `setColorfulLight` (code 1) — plays a pre-baked animation loaded from
  on-device XML style resources; it does not accept arbitrary colors and
  only works for `styleType` values present in `isSupportLedStripScene()`
  (see `docs/FINDINGS.md`).

## The working call

```java
setCustomLight(
    color,              // ARGB int, e.g. 0xFFFF0000 for red
    flashMode,           // 0 = solid, no hardware blink
    onMs,                // e.g. 500 — see timing note below
    offMs,               // 0
    brightNessMode,      // 0
    "com.android.camera",  // MUST be exactly this string — see docs/BINDER.md
    12,                  // styleType — LIGHTSTYLE_CAMERA
    0                    // userId
);
```

### Timing note (important, hard-won the hard way)

`onMs` is not just cosmetic — it interacts with how fast you can send new
colors. If you send a new color every N ms but `onMs` is much larger than N
(e.g. `onMs=500` while updating every 40–80ms for a rainbow animation), the
light service's internal timer can cause the ring to appear stuck on the
first color instead of animating smoothly. **Match `onMs` to your animation
step interval** (e.g. both ~50–60ms) for smooth per-frame color updates.

## `styleType` values seen in the decompiled code

No named constants exist in the server code — these are all magic numbers.
Confirmed meanings (from `HyperLightsService` + `android.app.ColorLightManager`
field dump):

| styleType | Meaning |
|---|---|
| -1 | reset / clear current scene |
| 1  | incoming call |
| 2  | game lighting |
| 3  | music rhythm (only styleType `setColorCommon` responds to) |
| 4  | alarm (`com.android.deskclock`) |
| 9  | notifications |
| 10 | (unconfirmed — appears in `isSupportLedStripScene` scene array) |
| **12** | **camera ring — the one this repo is about** |
| 13 | voice assistant |

`android.app.ColorLightManager` additionally exposes these as public static
`LIGHTSTYLE_*` int fields (same underlying values): `LIGHTSTYLE_ALARM`,
`LIGHTSTYLE_BATTERY`, `LIGHTSTYLE_CAMERA`, `LIGHTSTYLE_DEFAULT`,
`LIGHTSTYLE_EXPAND`, `LIGHTSTYLE_INTERRUPT`, `LIGHTSTYLE_LED`,
`LIGHTSTYLE_LED_BOOTUP`, `LIGHTSTYLE_LED_BREAK`,
`LIGHTSTYLE_LED_SATELLITEOFF`, `LIGHTSTYLE_LED_SATELLITEON`,
`LIGHTSTYLE_LIGHT_ID_PRIVACY`, `LIGHTSTYLE_LUCKYMONEY`, `LIGHTSTYLE_MUSIC`,
`LIGHTSTYLE_NOTIFICATION`, `LIGHTSTYLE_PHONE`, `LIGHTSTYLE_SRC_NOTFOUND`,
`LIGHTSTYLE_TURNOFF`, `LIGHTSTYLE_VOICE_ASSISTANT`.

## `flashMode` / `brightNessMode`

Not defined in `miui-services.jar` or `miui-framework.jar` — these are
passed straight through to the standard AOSP `LightState`/light HAL layer.
Believed (not verified against this specific build's `framework.jar`) to
follow the usual Android values:

- `flashMode`: `0` = none/solid, `1` = timed blink (`onMs`/`offMs`), `2` = hardware blink
- `brightNessMode`: `0` = user, `1` = sensor, `2` = low persistence

## `android.app.ColorLightManager`

A separate client-side class, **not present in `framework.jar`'s dex** —
loadable via plain `Class.forName("android.app.ColorLightManager")` on this
device without any hidden-API bypass, but its actual defining jar was not
located. It exposes the same four setters plus:

```java
boolean isSupportLedStrip();      // static
void enableBatteryLight(boolean)
void enableLight(boolean)
void enableMusicLight(boolean)
void enableNotificationLight(boolean)
void enableTimeLight(boolean)
boolean isBatteryLightEnable()
boolean isLightEnable()
boolean isLightTimeEnable()
boolean isMusicLightEnable()
boolean isNotificationLightEnable()
void setColorfullLightEndEnableTime(long, long)
void setColorfullLightStartEnableTime(long, long)
```

See `docs/BINDER.md` for how to obtain a `ColorLightManager`/`ILightsManager`
instance in practice.
