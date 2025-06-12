# QRZen - Barcode & QR Code Scanner for Jetpack Compose

QRZen is a modern Jetpack Compose-based QR and Barcode scanner library for Android. Built with extensibility, performance, and composability in mind, QRZen leverages Google's CameraX and ZXing (Zebra Crossing) under the hood to deliver fast and reliable scanning experiences.

---

## Features

* ✅ Jetpack Compose Composable APIs
* ✨ Bordered scan box UI (with blend mode cutout)
* 🔦 Flashlight (torch) toggle
* ↕️ Slider zoom control
* 🌍 Tap-to-focus support
* ✅ Barcode formats powered by ZXing
* ⛔ Permission handler included

---

## Installation

QRZen is available via [JitPack](https://jitpack.io/). Add the following to your `build.gradle`:

```kotlin
// root-level build.gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Then add the dependency:

```kotlin
// app-level build.gradle
implementation("com.github.dontkeep:QRZen:<latest-version>")
```

Replace `<latest-version>` with the latest commit hash or tag.

---

## Usage

### Basic Setup

Start by requesting camera permission using the built-in handler:

```kotlin
CameraPermissionHandler { granted ->
    if (granted) {
        // Show scanner UI
    } else {
        PermissionDeniedMessage()
    }
}
```

### ZenScannerScreen

A simple fullscreen scanner with optional zoom, tap-to-focus, and flash support:

```kotlin
ZenScannerScreen(
    isScanningEnabled = true,
    onQrCodeScanned = { result -> /* handle result */ },
    isFlashEnabled = true,
    isZoomEnabled = true,
    isTapToFocusEnabled = true
)
```

### BorderQRScanner

A more visual scanner with a highlighted border scan area:

```kotlin
BorderQRScanner(
    isScanningEnabled = true,
    onQrCodeScanned = { result -> /* handle result */ },
    isFlashEnabled = true,
    isZoomEnabled = true,
    isTapToFocusEnabled = true
)
```

---

## Under the Hood

* **CameraX** powers camera preview and control.
* **ZXing** (Zebra Crossing) handles barcode decoding logic.
* **ImageAnalysis** from CameraX feeds frames to `CoreScanner`, which reads and parses barcodes efficiently.

---

## License

```
MIT License

Copyright (c) 2025 dontkeep

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so.

```

---

## Contributions

Contributions, issues, and feature requests are welcome!
Feel free to open a PR or issue in the [GitHub repository](https://github.com/dontkeep/QRZen).

---

Happy Scanning 🚀
