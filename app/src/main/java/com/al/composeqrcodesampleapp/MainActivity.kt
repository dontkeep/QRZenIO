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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.al.qrzen.scanner.BorderQRScanner
import com.al.qrzen.permissionhandler.CameraPermissionHandler
import com.al.qrzen.permissionhandler.PermissionDeniedMessage
import com.al.qrzen.scanner.ZenScannerScreen
import com.al.qrzen.ui.AlertShowResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

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
    var isScanningEnabled by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var useBorderScanner by remember { mutableStateOf(true) } // Toggle flag

    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (hasCameraPermission) {
            true -> {
                if (isScanningEnabled) {
                    if (useBorderScanner) {
                        BorderQRScanner(
                            modifier = Modifier.fillMaxSize(),
                            isScanningEnabled = isScanningEnabled,
                            isFlashEnabled = true,
                            isZoomEnabled = true,
                            isTapToFocusEnabled = true,
                            onQrCodeScanned = { result ->
                                if (isProcessing) return@BorderQRScanner
                                isProcessing = true
                                Log.d("QR Code", "Scanned result: $result")
                                scannedResult = result
                                isScanningEnabled = false
                                isProcessing = false
                            }
                        )
                    } else {
                        ZenScannerScreen(
                            modifier = Modifier.fillMaxSize(),
                            isScanningEnabled = isScanningEnabled,
                            isFlashEnabled = true,
                            isZoomEnabled = true,
                            isTapToFocusEnabled = true,
                            onQrCodeScanned = { result ->
                                if (isProcessing) return@ZenScannerScreen
                                isProcessing = true
                                Log.d("QR Code", "Scanned result: $result")
                                scannedResult = result
                                isScanningEnabled = false
                                isProcessing = false
                            }
                        )
                    }
                }
            }

            false -> PermissionDeniedMessage()
            null -> {
                Text("Requesting permission...")
            }
        }

        Button(
            onClick = {
                useBorderScanner = !useBorderScanner
                scannedResult = null
                isScanningEnabled = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = 32.dp,
                    end = 16.dp,
                )
        ) {
            Text(if (useBorderScanner) "Switch to QRZen" else "Switch to Border")
        }

        scannedResult?.let {
            AlertShowResult(result = it) {
                scannedResult = null
                isScanningEnabled = true
            }
        }
    }
}
