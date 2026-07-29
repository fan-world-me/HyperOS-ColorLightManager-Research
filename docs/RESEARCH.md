# Methodology

## Tools used

- **jadx** (CLI) — decompiling `miui-services.jar`, `miui-framework.jar`,
  and `framework.jar` pulled directly off the device (`cat
  /system_ext/framework/*.jar` / `cat /system/framework/framework.jar`
  via a Shizuku/root shell, since these are otherwise-readable system
  files — no exploit needed, just file read access).
- **Shizuku** — for both a shell (`rish`) to run `service list`,
  `dumpsys`, and pull framework jars, and later as the runtime binder
  bridge for the actual app.
- A separate independent **blind Binder transaction fuzzer** app — used
  as a sanity check against the manually decompiled method table (see
  `docs/TIMELINE.md` step 6). Useful technique in general: if you don't
  trust a decompile, throw parameters at transaction codes 1..N and see
  what doesn't throw.
- **apksigner / jarsigner / zipalign / aapt2** (both the official Android
  SDK build-tools versions and Termux's own bionic-compiled `aapt2`
  package — the two are not interchangeable, see the note below) for
  diagnosing a broken release build.

## Where to look on a HyperOS device

- `/system_ext/framework/miui-services.jar` — server-side
  `*Service` implementations.
- `/system_ext/framework/miui-framework.jar` — client-facing AIDL
  interfaces / Manager wrapper classes.
- `/system/framework/framework.jar` — base AOSP framework, patched with
  Xiaomi-specific bridge methods in places (see
  `INotificationManager.getColorLightManager()` in `docs/BINDER.md`).
- `service list` — lists every registered Binder service by name; grep
  for anything relevant rather than guessing names.
- `dumpsys <servicename>` — often (not always) prints useful internal
  state.

## A note on building on-device (Termux)

Termux's userland is Android's own Bionic libc, not glibc — a standard
prebuilt Linux binary (like the `aapt2` that AGP/Gradle downloads from
Google's servers) will not execute inside Termux
(`Exec format error` / a shell trying to interpret the ELF as a script).
Fix: `pkg install aapt aapt2` (Termux's own bionic-compiled build) and
point Gradle at it explicitly:

```
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

## A note on installing a freshly built APK

`pm install /sdcard/...` from a Shizuku shell can fail with

```
System server has no access to read file context u:object_r:fuse:s0
```

because `system_server` can't read through the FUSE layer backing
`/sdcard`. Copy the APK to `/data/local/tmp/` first (from within the
Shizuku shell itself, which does have access to both locations) and
install from there instead.

## A note on class-name collisions

If you're writing your own local Java/Kotlin proxy for a hidden AIDL
interface that also exists in the platform's own boot classpath (which
`miui.lights.ILightsManager` does — it's real, shipped in
`miui-framework.jar`), **do not** name your local class with the exact
same fully-qualified name. Android's classloader delegates to the parent
(boot classloader) first; your app's own class gets silently shadowed by
the system's real one, which may have a different/absent method set,
producing a `NoSuchMethodError` at runtime that has nothing to do with
permissions or Binder itself. Put your proxy under your own app's
package — only the Binder `DESCRIPTOR` string has to match the real
service, not the Java package of your local interface. See
`docs/TIMELINE.md` step 8 for how this one cost a full debugging cycle.
