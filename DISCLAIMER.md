# Disclaimer

## Research Purpose

This repository contains **original research, documentation, and open-source code** produced through independent reverse engineering of publicly distributed firmware for personal educational and interoperability purposes.

## No Proprietary Files Redistributed

**No proprietary Xiaomi, POCO, or Redmi files are included in this repository.** This means:

- No firmware images (`.zip`, `.tgz`, boot/system partitions)
- No APK or JAR files extracted from MIUI / HyperOS
- No compiled binaries from Xiaomi's framework

If you need `framework.jar`, `miui-services.jar`, or any other system file for your own analysis, extract them from **your own device** via ADB:

```bash
adb pull /system/framework/framework.jar
adb pull /system/framework/miui-services.jar
```

## Legality

Reverse engineering for interoperability purposes is explicitly permitted under the EU Software Directive (2009/24/EC) and is broadly protected under fair use / fair dealing doctrines in many jurisdictions.

This project does not circumvent any DRM or access control measures. It does not enable piracy or facilitate unauthorized access to any service.

## No Warranty

This software and documentation are provided **"as is"**, without warranty of any kind, express or implied. The authors are not responsible for any damage to your device, data loss, or any other consequences of using information or code from this repository.

## Trademarks

Xiaomi, POCO, Redmi, MIUI, and HyperOS are trademarks of Xiaomi Inc. and its affiliates. This project is **not affiliated with, endorsed by, or sponsored by Xiaomi** in any way.

All other trademarks belong to their respective owners.

## DMCA / Takedown

If you believe any content in this repository infringes your intellectual property rights, please open an issue or contact the maintainer directly before filing a DMCA notice so the matter can be resolved quickly.
