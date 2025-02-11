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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    CameraPermissionHandler { granted ->
        hasCameraPermission = granted
    }
    when (hasCameraPermission) {
        true -> {
            QRCodeScannerScreen(
                modifier = Modifier.fillMaxSize(),
                onQrCodeScanned = { result ->
                    scannedResult = result
                }
            )
        }
        false -> {
            PermissionDeniedMessage()
        } else -> {
            null
        }
    }
    scannedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { scannedResult = null },
            title = { Text(text = "Scan Result") },
            text = { Text(text = result) },
            confirmButton = {
                Button(onClick = { scannedResult = null }) {
                    Text("OK")
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

