# Binder access, permissions, and the caller-check quirk

## Service registration

```
$ service list | grep -i light
55      android.hardware.light.ILights/default: []
236     lights: [android.hardware.lights.ILightsManager]
279     miui.lights.ILightsManager: [miui.lights.ILightsManager]
```

Three separate things live under "lights", and they are **not**
interchangeable:

- `android.hardware.light.ILights/default` — the raw HAL. Never reachable
  by apps directly, only by `system_server`.
- `lights` → `android.hardware.lights.ILightsManager` — standard AOSP
  `LightsManager`/`SystemLightsManager`. Only knows the fixed AOSP `LightId`
  set (keyboard, buttons, battery, notification LED, attention, bluetooth,
  wifi). **The camera ring is not among them.** `dumpsys lights` only
  dumps this one.
- `miui.lights.ILightsManager` — the actual Xiaomi-added service this repo
  is about.

## Server-side implementation

On this HyperOS build (`HyperLightsService`, not the older
`MiuiLightsService` also present in the same jar — check which one your
build actually registers), the caller check is inconsistent between
methods:

```java
// used by setColorfulLight — checks the REAL caller
private boolean checkCallerVerify(String callingPackage) {
    if (callingPackage == null) return false;
    int uid = Binder.getCallingUid();
    int appid = UserHandle.getAppId(uid);
    if (appid == 1000 || appid == 1001 || appid == 1013 || uid == 0 || uid == 2000) {
        return true; // system / root / shell(!)
    }
    return mContext.getPackageManager()
        .checkPermission(PERMISSION_ACCESS_SET_LIGHTS, callingPackage) == 0;
}

// used by setCustomLight — does NOT check the real caller at all
private boolean checkCustomLightCaller(String pkg) {
    if (pkg != null) {
        return "com.android.camera".equals(pkg) || VOICE_ASSISTANT_PKG.equals(pkg);
    }
    return false;
}
```

`checkCustomLightCaller` only compares the **string value of the `pkg`
argument you pass in the Parcel** — it never calls `Binder.getCallingUid()`.
Since `pkg` is just a caller-supplied `String` parameter (not derived from
the actual calling identity), any caller can pass the literal string
`"com.android.camera"` and pass this check regardless of who they really
are. This looks like an oversight (the sibling method `checkCallerVerify`
does check the real UID) rather than an intentional trust boundary, but the
practical effect is the same: **`setCustomLight` has no real caller
restriction.**

`PERMISSION_ACCESS_SET_LIGHTS` resolves to the string
`com.miui.permission.ACCESS_SET_LIGHTS` — a signature-level permission a
normal third-party app cannot hold, but note it's irrelevant for
`setCustomLight` specifically, since that path never checks it.

## `settings_strip_light_enable`

There's one `Settings.Global` int key that gates a `forceControlLight` flag
inside `setCustomLight`'s server-side handling:

```
settings put global settings_strip_light_enable 1
```

The call still reaches `LedStrip.setLedStrip(...)` even without this being
set (the `else` branch just passes `forceControlLight=false`), but setting
it is recommended for reliable behavior. This can be set via `adb shell
settings` or Shizuku shell without root.

## Three ways to get the `IBinder`

### 1. Shizuku, by service name (confirmed working)

```kotlin
val binder = ShizukuBinderWrapper(
    SystemServiceHelper.getSystemService("miui.lights.ILightsManager")
)
val lights = ILightsManager.Stub.asInterface(binder)
```

Because Shizuku's `ShizukuBinderWrapper` makes the transaction appear to
come from the shell process (uid 2000), any method that *does* check
`Binder.getCallingUid()` (like `checkCallerVerify`) also passes — shell uid
is explicitly allow-listed.

### 2. Root

Same idea, but the calling process needs to bypass Android's hidden-API
reflection enforcement itself (this blocks plain
`ServiceManager.getService(...)` reflection from a normally-launched app
process on API 28+). Typically done by spawning your code via
`su -c app_process ...` instead of the normal Zygote-forked app process, or
by making the app a signed system/priv-app.

### 3. Piggyback via `NotificationManager` (new finding, no Shizuku/root needed to *obtain* the binder)

`framework.jar` defines an extra method bolted onto the otherwise-standard
notification manager AIDL:

```java
// android/app/INotificationManager.java
IBinder getColorLightManager() throws RemoteException;   // transaction code 163
```

implemented server-side in `NotificationManagerServiceImpl` as:

```java
public IBinder getColorLightManager() {
    return ((MiuiLightsManagerInternal) LocalServices.getService(MiuiLightsManagerInternal.class))
            .getBinderService();
}
```

with **no permission check at all**. Since `"notification"` is a completely
ordinary system service every app can already reach via
`Context.getSystemService(Context.NOTIFICATION_SERVICE)`, this route
doesn't require Shizuku or root just to *obtain* the same
`miui.lights.ILightsManager` binder — only ordinary reflection on your own
`NotificationManager` object (to read its private `mService` field and
invoke `getColorLightManager()` on it). This has not yet been fully tested
end-to-end on-device — see the open item in `docs/RESEARCH.md`. Note that
even via this route, `setCustomLight`'s caller check (see above) still
requires passing `pkg="com.android.camera"`.

## Hardware backing the ring

```
/sys/devices/platform/soc/11281000.i2c/i2c-11/11-0030/leds/aw21024_led
/sys/bus/i2c/drivers/aw21024_led
/sys/module/leds_color21024
```

Confirms an **Awinic AW21024** (24-channel I²C RGB LED driver) behind the
ring. Direct sysfs access is blocked by SELinux even under a Shizuku shell
(`Permission denied` on the leds directory) — only reachable through the
framework path documented above.
