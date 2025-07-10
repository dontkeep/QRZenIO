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
pluginManagement {
    repositories {
        //...other repo
        maven { url 'https://jitpack.io' }
    }
}

dependenciesResolutionManagement {
    repositories {
        //...other repo
        maven { url = uri("https://jitpack.io") }
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
    modifier = Modifier.fillMaxSize(),
    isScanningEnabled = isScanningEnabled,
    isFlashEnabled = true,
    isZoomEnabled = true,
    isTapToFocusEnabled = true,
    onQrCodeScanned = { result ->
        if (isProcessing) return@ZenScannerScreen
        isProcessing = true
        // handle the result
        isScanningEnabled = false
        isProcessing = false
    }
)
```

### BorderQRScanner

A more visual scanner with a highlighted border scan area:

```kotlin
BorderQRScanner(
    modifier = Modifier.fillMaxSize(),
    isScanningEnabled = isScanningEnabled,
    isFlashEnabled = true,
    isZoomEnabled = true,
    isTapToFocusEnabled = true,
    onQrCodeScanned = { result ->
        if (isProcessing) return@BorderQRScanner
        isProcessing = true
        // handle result here
        isScanningEnabled = false
        isProcessing = false
    }
)
```

---

## Under the Hood

* **CameraX** powers camera preview and control.
* **ZXing** (Zebra Crossing) handles barcode decoding logic.
* **ImageAnalysis** from CameraX feeds frames to `CoreScanner`, which reads and parses barcodes efficiently.

---

## Contributions

Contributions, issues, and feature requests are welcome!
Feel free to open a PR or issue in the [GitHub repository](https://github.com/dontkeep/QRZen).

---

Happy Scanning 🚀
