# HyperOS ColorLightManager Research

> Reverse engineering research of the RGB camera ring LED system on **POCO X8 Pro** / **Redmi Turbo 5** (codename: `klee`).

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Status: WIP](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)]()
[![Platform: HyperOS](https://img.shields.io/badge/Platform-HyperOS%2FMiUI-orange)]()

---

## Overview

This repository documents ongoing reverse engineering of the **ColorLightManager** subsystem in Xiaomi's HyperOS / MIUI framework. The goal is to fully understand and replicate the Binder-based API that controls the RGB LED ring surrounding the rear camera module on supported devices.

The research covers:

- **AW21024** — the Awinic RGB LED controller chip used in the hardware
- **ColorLightManager** — a proprietary Xiaomi framework component managing LED patterns and states
- **HyperLightsService** — the system service brokering LED access
- **ILightsManager** — the AIDL / Binder interface exposed to privileged callers
- Hidden API surface accessible via Shizuku or root

---

## Supported Devices

| Device | Codename | Status |
|---|---|---|
| POCO X8 Pro | klee | ✅ Primary research target |
| Redmi Turbo 5 | klee | ✅ Same hardware, same ROM |

Other HyperOS devices with camera ring LEDs may share a similar or identical interface — contributions welcome.

---

## Repository Structure

```
HyperOS-ColorLightManager-Research/
├── docs/
│   ├── API.md          # Known ILightsManager method signatures
│   ├── BINDER.md       # Binder interface, permissions, Shizuku notes
│   ├── FINDINGS.md     # Hardware and framework components identified
│   ├── RESEARCH.md     # Running summary of current findings
│   └── TIMELINE.md     # Chronological log of reverse engineering steps
├── src/                # Source code / PoC implementations (WIP)
├── examples/           # Usage examples and Shizuku integration snippets
├── scripts/            # Helper scripts for analysis
├── logs/               # Captured logcat / binder traces (sanitised)
├── DISCLAIMER.md       # Legal disclaimer — read before using anything here
├── SECURITY.md         # Responsible disclosure policy
├── CONTRIBUTING.md     # How to contribute
├── CODE_OF_CONDUCT.md  # Community standards
├── CHANGELOG.md        # What changed and when
└── LICENSE             # GNU General Public License v3.0
```

---

## Key Findings

### Hardware

- LED controller chip: **AW21024** (Awinic, 24-channel RGB LED driver)
- Controlled via I²C from the SoC; brightness, color and patterns are driven by the framework layer

### Framework Components

| Component | Location | Role |
|---|---|---|
| `ColorLightManager` | `framework.jar` | High-level manager, orchestrates LED states |
| `HyperLightsService` | `miui-services.jar` | System service, enforces permissions |
| `ILightsManager` | AIDL / Binder | Interface used by clients to call into the service |

### Known API Methods

```java
// ILightsManager Binder interface — miui.lights.ILightsManager
void setColorfulLight(int styleType, int[] colors, int speed, int brightness);
void setColorCommon(int r, int g, int b, int brightness);
void setColorLed(int ledIndex, int r, int g, int b);
void setCustomLight(int[] pattern);
```

- `styleType 12` → camera ring lighting mode
- Full parameter semantics are still being mapped — see [`docs/API.md`](docs/API.md)

### Access Methods

- Calling these methods requires elevated permissions not granted to normal apps
- **Shizuku** is the primary target for rootless access
- Root / ADB shell can call the Binder directly via `service call`

---

## Goals

- [x] Identify LED controller chip (AW21024)
- [x] Locate `ColorLightManager` in `framework.jar`
- [x] Locate `HyperLightsService` in `miui-services.jar`
- [x] Identify `ILightsManager` Binder interface
- [x] Map known method signatures
- [ ] Fully document all method parameters and enums
- [ ] Build working PoC via Shizuku (rootless)
- [ ] Build open-source client library
- [ ] Test on other HyperOS devices with camera ring LEDs

---

## Getting Started

**You cannot run any of this without the proprietary Xiaomi framework.** No firmware files or APKs are redistributed in this repository — see [`DISCLAIMER.md`](DISCLAIMER.md).

To reproduce the analysis:

1. Extract `framework.jar` and `miui-services.jar` from your own device (ADB pull from `/system/framework/`)
2. Decompile with [jadx](https://github.com/skylot/jadx) or [apktool](https://apktool.org/)
3. Search for `ColorLightManager`, `HyperLightsService`, `ILightsManager`
4. See [`docs/RESEARCH.md`](docs/RESEARCH.md) for analysis notes and pointers

---

## Contributing

Contributions are very welcome — especially from owners of other HyperOS devices with camera ring LEDs.

Please include in your PR / issue:
- Device model and codename
- HyperOS / MIUI version
- Android version
- Relevant logcat output
- Decompiled class names / method signatures if applicable

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for full guidelines.

**Do not upload proprietary Xiaomi binaries, firmware images, or APKs.**

---

## Legal

This project is licensed under the **GNU General Public License v3.0** — see [`LICENSE`](LICENSE) for the full text.

All research is conducted on firmware extracted from personally-owned devices. No proprietary files are redistributed. See [`DISCLAIMER.md`](DISCLAIMER.md) for the full disclaimer.

---

## Disclaimer

This is an independent community research project. It is not affiliated with, endorsed by, or sponsored by Xiaomi, POCO, or any related entity. All trademarks belong to their respective owners.
