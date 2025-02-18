package com.al.composeqrcodesampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.al.composeqrcodesampleapp.ui.theme.ComposeQRCodeSampleAppTheme
import android.Manifest.permission.CAMERA
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import java.io.IOException
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.driver.UsbSerialPort

var usbSerialPort: UsbSerialPort? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initUsbSerial(this)
        setContent {
            ComposeQRCodeSampleAppTheme {
                MainScreen()
            }
        }
    }
}

fun initUsbSerial(context: Context) {
    val usbManager = getSystemService(context, UsbManager::class.java)
    if (usbManager == null) {
        Log.d("USB", "UsbManager is null")
        return
    }
    val deviceList = usbManager.deviceList
    Log.d(
        "USB",
        "Connected USB devices: ${deviceList.values.joinToString { "VendorID: ${it.vendorId}, ProductID: ${it.productId}" }}"
    )

    val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    if (availableDrivers.isEmpty()) {
        Log.d("USB", "No USB drivers found")
        return
    }

    val driver = availableDrivers[0]
    val connection = usbManager.openDevice(driver.device)
    if (connection == null) {
        Log.d("USB", "USB permission not granted for device")
        val ACTION_USB_PERMISSION = "com.al.composeqrcodesampleapp.USB_PERMISSION"
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_MUTABLE if required for your target API level.
        )

        val usbPermissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_USB_PERMISSION) {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            // Permission granted; try opening the connection again.
                            val connection = usbManager.openDevice(driver.device)
                            if (connection != null) {
                                val port = driver.ports[0]
                                try {
                                    port.open(connection)
                                    port.setParameters(
                                        115200,
                                        8,
                                        UsbSerialPort.STOPBITS_1,
                                        UsbSerialPort.PARITY_NONE
                                    )
                                    usbSerialPort = port
                                    Log.d(
                                        "USB",
                                        "USB port opened successfully after permission granted"
                                    )
                                } catch (e: IOException) {
                                    Log.e("USB", "Error opening USB port", e)
                                }
                            } else {
                                Log.d("USB", "Connection still null even after permission granted")
                            }
                        } else {
                            Log.d("USB", "Permission denied for USB device")
                        }
                    }
                    // Unregister the receiver once permission has been handled.
                    context?.unregisterReceiver(this)
                }
            }
        }

        // Register the receiver to listen for the permission response.
        ContextCompat.registerReceiver(
            context,
            usbPermissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // Request permission for the device.
        usbManager.requestPermission(driver.device, permissionIntent)

        return
    }

    val port = driver.ports[0]
    try {
        port.open(connection)
        port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        usbSerialPort = port
        Log.d("USB", "USB port opened successfully")
    } catch (e: IOException) {
        Log.e("USB", "Error opening USB port", e)
    }
}

fun sendDataToArduino(data: String, onResult: (Boolean, String) -> Unit) {
    val port = usbSerialPort
    if (port == null) {
        onResult(false, "USB port is not available")
        return
    }
    try {
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        Log.d("USB", "Sending data: $data")
        val byteWritten = dataBytes.size
        port.write(dataBytes, 1000)
        onResult(true, "Wrote $byteWritten bytes")
    } catch (e: IOException) {
        onResult(false, e.message ?: "IOException occurred")
    }
}

@Composable
fun MainScreen() {
    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var transferStatus by remember { mutableStateOf<String?>(null) }
    var isSendingData by remember { mutableStateOf(false) }
    var isScanningEnabled by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) } // New flag to track processing state

    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }

    when (hasCameraPermission) {
        true -> {
            if (isScanningEnabled) {
                QRCodeScannerScreen(
                    modifier = Modifier.fillMaxSize(),
                    isScanningEnabled = isScanningEnabled, // Pass the state to the scanner
                    onQrCodeScanned = { result ->
                        if (isProcessing) return@QRCodeScannerScreen // Prevent multiple processing
                        isProcessing = true
                        Log.d("QR Code", "Scanned result: $result")
                        scannedResult = result
                        isSendingData = true
                        isScanningEnabled = false // Disable scanner immediately
                        sendDataToArduino(result) { success, message ->
                            transferStatus = if (success) {
                                "Data transferred successfully:\n$message"
                            } else {
                                "Error transferring data:\n$message"
                            }
                            isSendingData = false
                            scannedResult = null // Dismiss scan result dialog
                            isProcessing = false // Reset processing flag
                        }
                    }
                )
            }
        }
        false -> PermissionDeniedMessage()
        else -> Unit
    }

    scannedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { scannedResult = null }, // Removed isScanningEnabled toggle
            title = { Text("Scan Result") },
            text = { Text(result) },
            confirmButton = {
                Button(onClick = { scannedResult = null }) {
                    Text("OK")
                }
            }
        )
    }

    transferStatus?.let { status ->
        AlertDialog(
            onDismissRequest = {
                transferStatus = null
                isScanningEnabled = true // Re-enable scanner only here
            },
            title = { Text("Transfer Status") },
            text = { Text(status) },
            confirmButton = {
                Button(onClick = {
                    transferStatus = null
                    isScanningEnabled = true // Ensure enable on OK click
                }) {
                    Text("OK")
                    Log.d("Error Status: ", status)
                }
            }
        )
    }
}


@Composable
fun CameraPermissionHandler(onPermissionResult: (Boolean) -> Unit) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        if (!permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(CAMERA)
        }
    }
}

@Composable
fun PermissionDeniedMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Camera permission is required to scan QR codes.")
    }
}

