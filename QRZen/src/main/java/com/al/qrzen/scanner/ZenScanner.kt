package com.al.qrzen.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import zxingcpp.BarcodeReader

@Composable
fun ZenScannerScreen(
    modifier: Modifier = Modifier,
    isScanningEnabled: Boolean,
    onQrCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = BarcodeReader()

}
