package com.al.qrzen.scanner

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import zxingcpp.BarcodeReader
import com.al.qrzen.R

@Composable
fun ZenScannerScreen(
    modifier: Modifier = Modifier,
    isScanningEnabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
    isFlashEnabled: Boolean = false
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scanner = BarcodeReader()
    var flashEnabled by remember { mutableStateOf(isFlashEnabled) }
    var camera: Camera? by remember { mutableStateOf(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val scannerView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(scannerView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) {
                        if (isScanningEnabled) {
                            val resultText = processImageProxy(it, scanner)
                            if (resultText.isNotEmpty() && isWithinScanArea(resultText)) {
                                onQrCodeScanned(resultText)
                            }
                        }
                        it.close()
                    }

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                        camera?.cameraControl?.enableTorch(flashEnabled)
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                scannerView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Dark overlay with transparent scanning area
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val overlayColor = Color.Black.copy(alpha = 0.6f)
                drawRect(overlayColor)
                val scanSize = 200.dp.toPx()
                val centerX = size.width / 2 - scanSize / 2
                val centerY = size.height / 2 - scanSize / 2
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(centerX, centerY),
                    size = Size(scanSize, scanSize),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }

        // Scanning area border
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
        )

        // Flash toggle button with an icon
        IconButton(
            onClick = {
                flashEnabled = !flashEnabled
                camera?.cameraControl?.enableTorch(flashEnabled)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Icon(
                painter = painterResource(id = if (flashEnabled) R.drawable.torch_filled else R.drawable.torch_line),
                contentDescription = "Flash Toggle",
                tint = Color.Black
            )
        }
    }
}

// Function to check if the scanned QR code is inside the scan area
private fun isWithinScanArea(resultText: String): Boolean {
    // Implement logic to verify if the QR code is within the overlay region
    return true // Placeholder
}


fun processImageProxy(image: ImageProxy, scanner: BarcodeReader): String {
    return image.use {
        scanner.read(it)
    }.joinToString("\n") { result ->
        "${result.text}"
    }
}