# (QRZen) QR Scanner & USB Serial Library

This library provides a modular solution that seamlessly integrates QR code scanning with USB serial communication for Android applications. It offers composable components for camera setup, permission handling, and real-time QR code detection, while maintaining USB data serialization and transfer in a separate module. This design allows you to easily integrate QR scanning capabilities and communicate with USB-connected devices (e.g., Arduino) within your app.

## Features

- **QR Code Scanning:**  
  Quickly integrate camera preview and QR code detection using Jetpack Compose.

- **Camera Permission Handling:**  
  Built-in composables to request and handle camera permissions.

- **USB Serial Communication:**  
  Separate module to manage USB device initialization and data transfer.

- **Modular Design:**  
  Separate packages for scanner and USB functionalities for ease of maintenance and reuse.

## Installation

### 1. Add the JitPack Repository

First, add JitPack to your project's repositories. In your root `build.gradle` (or `settings.gradle` for newer Gradle versions), include:

```groovy
allprojects {
    repositories {
        // ... other repositories
        maven { url 'https://jitpack.io' }
    }
}
```
2. Add the Library Dependency
Next, add the dependency for the QR Scanner & USB Serial Library in your app's build.gradle file:

```groovy
dependencies {
    implementation 'com.github.dontkeep:QRZen:v1.0.1'
    // Other dependencies...
}
```

## Usage

### QR Code Scanning

Use the `QRCodeScannerScreen` composable along with the `CameraPermissionHandler` to scan QR codes:

```kotlin
@Composable
fun MainScreen() {
    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }

    // Request camera permission
    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }

    when (hasCameraPermission) {
        true -> {
            QRCodeScannerScreen(
                modifier = Modifier.fillMaxSize(),
                isScanningEnabled = true,
                onQrCodeScanned = { result ->
                    // Handle scanned QR code result
                    Log.d("QR Code", "Scanned result: $result")
                }
            )
        }
        false -> {
            PermissionDeniedMessage()
        }
        else -> {
            // Optionally show a loading indicator or placeholder
        }
    }
}
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
  Composable for camera preview and QR code scanning.

- **scanner/CameraPermissionHandler.kt:**  
  Handles camera permission requests using Jetpack Compose.

- **scanner/PermissionDeniedMessage.kt:**  
  Displays a message if camera permission is not granted.

## Customization

Feel free to adjust the camera setup, QR code processing, or USB communication logic to suit your application's specific requirements.

## License

This project is licensed under the Apache License, Version 2.0.  
For details, see [http://www.apache.org/licenses/LICENSE-2.0.txt](http://www.apache.org/licenses/LICENSE-2.0.txt).
