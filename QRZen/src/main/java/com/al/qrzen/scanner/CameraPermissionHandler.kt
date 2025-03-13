package com.al.qrzen.scanner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun CameraPermissionHandler(
    onPermissionResult: (Boolean) -> Unit
) {
    var permissionResult by remember { mutableStateOf<Boolean>(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult(it)
    }

    LaunchedEffect(Unit) {
        if (!permissionResult) {
            permissionResult = true
            delay(100)
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
}