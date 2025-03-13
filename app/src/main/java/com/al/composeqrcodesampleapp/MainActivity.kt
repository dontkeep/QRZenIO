package com.al.composeqrcodesampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.al.composeqrcodesampleapp.ui.theme.ComposeQRCodeSampleAppTheme
import android.util.Log
import androidx.compose.material3.Text
import com.al.qrzen.scanner.BorderScanner
import com.al.qrzen.scanner.CameraPermissionHandler
import com.al.qrzen.scanner.PermissionDeniedMessage
import com.al.qrzen.ui.AlertShowResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        initUsbSerial(this)
        setContent {
            ComposeQRCodeSampleAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }
    var scannedResult by remember { mutableStateOf<String?>(null) }
//    var transferStatus by remember { mutableStateOf<String?>(null) }
    var isScanningEnabled by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }

    when (hasCameraPermission) {
        true -> {
            if (isScanningEnabled) {
                Log.d("QR Code", "${isScanningEnabled}")
                BorderScanner(
                    modifier = Modifier.fillMaxSize(),
                    isScanningEnabled = isScanningEnabled,
                    isFlashEnabled = true,
                    onQrCodeScanned = { result ->
                        if (isProcessing) return@BorderScanner
                        isProcessing = true
                        Log.d("QR Code", "Scanned result: $result")
                        scannedResult = result
                        isProcessing = false
                        isScanningEnabled = false

//                        UsbSerialManager.sendDataToArduino(result) { success, message ->
//                            transferStatus = if (success) {
//                                "Data transferred successfully:\n$message"
//                            } else {
//                                "Error transferring data:\n$message"
//                            }
//                            isScanningEnabled = true
//                            isProcessing = false
//                        }
                    }
                )
            }
        }
        false -> PermissionDeniedMessage()
        null -> {
            Text("Requesting permission...")
        }
    }
    Log.d("QR Code is not empty", "Scanned result: $scannedResult")
    scannedResult?.let {
        AlertShowResult(result = it) {
            scannedResult = null
            isScanningEnabled = true
        }
    }
}