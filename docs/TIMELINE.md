# Timeline

1. **Started from a dead end.** No public Xiaomi SDK, no docs, no working
   solution found on Reddit/XDA for controlling the POCO X8 Pro camera ring
   with custom effects. Initial workaround idea (driving the ring
   indirectly via the "music rhythm" audio-reactive mode using an inaudible
   tone) was shelved in favor of finding the real internal API.

2. **Decompiled `miui-services.jar` and `miui-framework.jar`.** Found
   `miui.lights.ILightsManager` (AIDL interface) and its server-side
   implementations `HyperLightsService` / `MiuiLightsService`.

3. **Found the caller-check inconsistency.** `setCustomLight`'s permission
   check (`checkCustomLightCaller`) only compares the caller-supplied
   `pkg` string against `"com.android.camera"` — it never calls
   `Binder.getCallingUid()`, unlike the sibling method
   `checkCallerVerify` used elsewhere in the same class. See
   `docs/BINDER.md`.

4. **Found the hardware.** `/sys/.../aw21024_led` — an Awinic AW21024
   I²C RGB driver — confirmed the physical chip behind the ring. Direct
   sysfs access turned out to be SELinux-blocked even under a Shizuku
   shell.

5. **First working call, via Shizuku.** Obtained the `IBinder` via
   `ServiceManager.getService("miui.lights.ILightsManager")` (wrapped
   through Shizuku's `ShizukuBinderWrapper`, so the call is seen as
   coming from `uid=2000`/shell), then called `setCustomLight(...)` with
   `pkg="com.android.camera"`, `styleType=12`. **The ring physically lit
   up** — confirmed visually, not just "Binder call didn't throw."

6. **Independent confirmation via blind fuzzing.** A separate tool that
   brute-forced Binder transaction codes 1–20 against the same service
   found exactly 4 responsive void methods on codes 1–4, matching the
   manually decompiled method table exactly.

7. **Found a second, permission-free path.** `framework.jar` defines
   `INotificationManager.getColorLightManager()` (transaction code 163),
   implemented server-side with **no permission check at all**, returning
   the same `miui.lights.ILightsManager` binder. Since `"notification"`
   is a completely ordinary service any app can reach via
   `Context.getSystemService(NOTIFICATION_SERVICE)`, this route doesn't
   need Shizuku/root just to *obtain* the binder (the `setCustomLight`
   caller check documented in step 3 still applies once you have it).
   Not yet fully tested end-to-end.

8. **Built and iterated a working app (`HaloLite`).** Went through
   several rounds of real bugs on real hardware:
   - a rainbow loop that stopped after a hardcoded 1.8s instead of
     running continuously,
   - "breathing"/"wave" effects built on an unverified guessed
     transaction code that was a silent no-op,
   - a foreground service that was written but never actually wired up
     to the UI, so nothing survived closing the app,
   - a classic Java classloader collision: naming the app's own local
     AIDL proxy class `miui.lights.ILightsManager` — identical to the
     real system class already present in `miui-framework.jar` — caused
     `NoSuchMethodError` at runtime, because Android's classloader
     delegates to the boot classpath first and silently resolved calls
     against the *system's* version of the class instead of the app's
     own bundled one. Fixed by moving the proxy to the app's own package
     namespace; the interface's Binder `DESCRIPTOR` string (which is what
     actually matters for the handshake) is independent of the Java
     package name.
   - a color update loop where the per-frame "hold" duration passed to
     `setCustomLight` equaled the update interval, causing visible
     on/off flicker between colors when IPC timing jitter let the
     hardware's own auto-off timer fire before the next update landed;
     fixed by decoupling "how often we send a new color" from "how long
     the hardware is told to hold it" (a generous hold value with margin
     over the update interval).

9. **Result:** a working standalone Android app, no root required (only
   Shizuku), that lights the camera ring in a full rainbow sweep on
   launch. As far as this repo's authors are aware, this is the first
   publicly documented working method for programmatically controlling
   this specific ring.

10. **Found the real fix for the flicker (not just a tuning knob).**
    `setCustomLight`'s server-side path always schedules a Handler-based
    auto-off timer, no matter how the `onMs`/`holdMs` ratio is tuned —
    every combination tried (equal to step interval, 1.3x, 8.5x, etc.)
    either flickered or froze on the first color, because it's the same
    race condition at different odds, not a parameter to dial away.
    `setColorCommon` with `styleType=3` (the same "music rhythm" light
    zone the stock audio-reactive feature drives) has **no timer at all**
    in its server-side implementation — an unconditional immediate state
    write. Switching to it eliminated the flicker completely at a 25ms
    update rate, no tuning required.

11. **Final working build:** a two-pass rainbow sweep (red → full spectrum
    → red → full spectrum → red, ~5 seconds total, no flicker), triggered
    automatically on app open, running in a foreground service so it
    survives the app being closed. This is the version documented as
    "confirmed working" throughout this repo.
