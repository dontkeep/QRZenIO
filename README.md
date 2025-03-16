# (QRZen) QR Scanner & USB Serial Library

This library provides a modular solution that seamlessly integrates QR code scanning with USB serial communication for Android applications. It offers composable components for camera setup, permission handling, and real-time QR code detection, while maintaining USB data serialization and transfer in a separate module. This design lets you easily integrate QR scanning capabilities and communicate with USB-connected devices (e.g., Arduino) within your app.

## Features

- **Normal QR Scanner (`ZenScannerScreen`)**  
  A simple QR scanner that integrates seamlessly into your app.

- **Bordered QR Scanner (`BorderScanner`)**  
  A QR scanner with a highlighted scanning area to focus user attention.

- **USB Serial Communication (`UsbSerialManager`)**  
  Enables communication with microcontroller devices via USB.

## Installation

### 1. Add the JitPack Repository

First, add JitPack to your project's repositories. In your root `settings.gradle` file, include:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add the Library Dependency
Next, add the dependency for the QRZen library in your app's `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("com.github.dontkeep:QRZen:v1.1.0")
}
```

## Usage

### Normal QR Scanner
Use the `ZenScannerScreen` composable to scan QR codes:

```kotlin
@Composable
fun MainScreen() {
    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }

    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }

    when (hasCameraPermission) {
        true -> {
            ZenScannerScreen(
                modifier = Modifier.fillMaxSize(),
                isScanningEnabled = true,
                isFlashEnabled = true,
                onQrCodeScanned = { result ->
                    Log.d("QR Code", "Scanned result: $result")
                }
            )
        }
        false -> PermissionDeniedMessage()
        null -> Text("Requesting permission...")
    }
}
```

### Bordered QR Scanner
Use the `BorderScanner` composable to provide a focused scanning area:

```kotlin
BorderScanner(
    modifier = Modifier.fillMaxSize(),
    isScanningEnabled = true,
    isFlashEnabled = true,
    onQrCodeScanned = { result ->
        Log.d("QR Code", "Scanned result: $result")
    }
)
```

### USB Serial Communication

#### 1. Initialize USB Connection
Call `UsbSerialManager.initUsbSerial(context)` in your `MainActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize USB functionality from the library.
        UsbSerialManager.initUsbSerial(this)
        
        setContent {
            MainScreen()
        }
    }
}
```

#### 2. Send Data
Use `UsbSerialManager.sendDataToArduino(...)` to send data to the connected USB device:

```kotlin
UsbSerialManager.sendDataToArduino("Your QR data or any string") { success, message ->
    if (success) {
        Log.d("USB", "Data sent successfully: $message")
    } else {
        Log.e("USB", "Error sending data: $message")
    }
}
```

## Library Structure

- **usb/UsbSerialManager.kt:**  
  Manages USB serial communication including initialization and data transfer.

- **scanner/ZenScanner.kt:**  
  Composable for a standard QR scanner.

- **scanner/BorderScanner.kt:**  
  Composable for a bordered QR scanner with a focused scanning area.

- **scanner/CameraPermissionHandler.kt:**  
  Handles camera permission requests using Jetpack Compose.

- **scanner/PermissionDeniedMessage.kt:**  
  Displays a message if camera permission is not granted.

## Customization

Feel free to adjust the camera setup, QR code processing, or USB communication logic to suit your application's specific requirements.

## License

This project is licensed under the Apache License, Version 2.0.  
For details, see [http://www.apache.org/licenses/LICENSE-2.0.txt](http://www.apache.org/licenses/LICENSE-2.0.txt).

