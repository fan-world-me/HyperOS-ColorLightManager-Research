# HyperOS ColorLightManager Research

Reverse-engineering the hidden Xiaomi/HyperOS API that controls the RGB
ring around the rear camera on the POCO X8 Pro (and likely shared
hardware on Redmi Turbo 5 / other `klee`-family devices).

**Status: working.** A confirmed, end-to-end, on-device tested method for
lighting the ring in custom colors exists — see below. As far as we know
this is the first publicly documented working method for this specific
hardware.

## TL;DR

```java
// via Shizuku, no root required
IBinder raw = SystemServiceHelper.getSystemService("miui.lights.ILightsManager");
ILightsManager lights = ILightsManager.Stub.asInterface(new ShizukuBinderWrapper(raw));

lights.setCustomLight(
    0xFFFF0000,            // ARGB color, e.g. red
    0,                      // flashMode — 0 = solid
    500,                    // onMs — see docs/API.md timing note
    0,                      // offMs
    0,                      // brightNessMode
    "com.android.camera",   // MUST be exactly this string — see docs/BINDER.md
    12,                     // styleType — camera
    0                       // userId
);
```

That's the entire working call. Everything else in this repo documents
how it was found, what doesn't work, and why.

## Repo layout

```
docs/
  API.md         — full method table, styleType values, timing notes
  BINDER.md       — service registration, permission-check quirk, how to get the IBinder
  FINDINGS.md     — hardware + component summary, confirmed dead ends
  RESEARCH.md     — tools/methodology, Termux build gotchas, classloader collision warning
  TIMELINE.md     — condensed step-by-step history of the investigation
src/
  main/java/miui/lights/ILightsManager.java
                  — the real interface, annotated (reference only — see warning inside)
examples/
  lightsapi/ILightsManager.java
                  — the SAME interface, safe to actually use (own package, no collision)
  LightsBridge.kt — Shizuku binder bridge, minimal
  HaloLightService.kt
                  — working foreground-service example: full rainbow sweep on the ring
```

## Quick start (if you just want to try it)

1. Install [Shizuku](https://shizuku.rikka.app/), start it (wireless
   debugging or ADB, no root needed).
2. Build a small app using `examples/lightsapi/ILightsManager.java` +
   `examples/LightsBridge.kt` as a starting point — grant it Shizuku
   permission at runtime.
3. Call `LightsBridge.setCameraRing(color, onMs)` from anywhere.

See `docs/RESEARCH.md` for the full toolchain notes (this was largely
built and tested on-device via Termux, no computer required).

## Why this was hard

No public Xiaomi SDK or documentation exists for this feature. The only
official-ish entry point (the "rhythmic light" music-reactive mode) only
reacts to audio, not arbitrary custom colors. Getting from there to a
working `setCustomLight` call meant decompiling three system jars,
finding a permission-check inconsistency, and debugging several
real on-device bugs along the way (see `docs/TIMELINE.md` for the full
account, including a classic Java classloader collision that cost a full
debugging cycle before being caught).

## Disclaimer

This uses an internal, undocumented, unsupported system API found via
reverse engineering. It works on the specific firmware version this was
tested against; Xiaomi can change or remove any of this in a future
update without notice. Shizuku is required (a legitimate, widely-used
tool); no root, no exploit, no modification of system files is involved
— everything here is a normal signed app talking to a system service it
was given permission to reach.
