# Findings summary

Device: POCO X8 Pro (codename `klee`), same hardware/ROM as Redmi Turbo 5.
ROM at time of research: HyperOS 3, Android 16 (SDK 36),
`MIUI OS3.0.306.0.WPJMIXM` (build string as reported by the device).

## Hardware

- RGB LED controller: **AW21024** (Awinic, 24-channel I²C RGB driver)
- I²C path: `/sys/devices/platform/soc/11281000.i2c/i2c-11/11-0030/leds/aw21024_led`
- Kernel module: `leds_color21024`
- Direct sysfs read/write is SELinux-denied even to a Shizuku (uid 2000)
  shell — must go through the framework Binder path, not the kernel node
  directly.

## Framework components located

| Component | Jar | Package |
|---|---|---|
| `ColorLightManager` | not found in `framework.jar`'s dex — loadable via `Class.forName` at runtime regardless; defining jar unidentified | `android.app` |
| `INotificationManager.getColorLightManager()` bridge | `framework.jar` | `android.app` |
| `ILightsManager` (AIDL) | `framework.jar` | `miui.lights` |
| `HyperLightsService` (active server implementation on this build) | `miui-services.jar` | `com.android.server.lights` |
| `MiuiLightsService` (older/alternate implementation, also present) | `miui-services.jar` | `com.android.server.lights` |
| `NotificationManagerServiceImpl` (implements the bridge method) | `miui-services.jar` | `com.android.server.notification` |
| `MiuiLightsManagerInternal` | `miui-services.jar` | `miui.app` |

## Registered services (`service list`)

```
55      android.hardware.light.ILights/default: []
236     lights: [android.hardware.lights.ILightsManager]
279     miui.lights.ILightsManager: [miui.lights.ILightsManager]
```

## Confirmed via independent blind Binder fuzzing

A separate tool that brute-forced transaction codes 1–20 against
`miui.lights.ILightsManager` (via Shizuku, uid 2000) with 8 parameter
templates each, found exactly **4 non-throwing void methods on codes 1–4**
and nothing else responsive on codes 5–20 — matching the manually
decompiled method table exactly (see `docs/API.md`). This is independent
confirmation that the interface really only exposes those 4 methods.

## Confirmed end-to-end working call

Via Shizuku (uid 2000), calling transaction code 4 (`setCustomLight`) with:

```
color        = <any ARGB>
flashMode    = 0
onMs         = matched to animation frame interval (see docs/API.md timing note)
offMs        = 0
brightNessMode = 0
pkg          = "com.android.camera"   (string check only — see docs/BINDER.md)
styleType    = 12
userId       = 0
```

**physically lights the camera ring in the selected color**, confirmed
visually on-device (not just "Binder call didn't throw"). This is, as far
as this repo's authors are aware, the first documented working call for
this specific ring.

## Known dead ends

- `setColorLed` (code 3): empty method body on this HyperOS build — always
  a silent no-op.
- `setColorCommon` (code 2) with arbitrary `styleType`: only does anything
  for `styleType==3` (music rhythm strip); anything else is a silent no-op.
- `setColorfulLight` (code 1): only plays pre-baked XML-defined animations
  for a fixed whitelist of `styleType` scene values; does not accept
  arbitrary custom colors.
- A generic/guessed `SET_EFFECT`-style transaction code (used for
  "breathing"/"wave" style effects in early client prototypes) is **not**
  one of the 4 real methods and does not control the ring — any client
  code using it needs to be rewritten to loop calls to the real
  `setCustomLight` instead.
- Direct sysfs access to the `aw21024_led` node: blocked by SELinux even
  under a Shizuku shell.
- `getSystemService(ColorLightManager.class)` returns `null` — this class
  is not registered through the standard `SystemServiceRegistry`, so it
  must be obtained by another means (constructor, or via the
  `getColorLightManager()` bridge — see `docs/BINDER.md`).
