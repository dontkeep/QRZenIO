package com.al.qrzen.scanner

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import zxingcpp.BarcodeReader
import com.al.qrzen.R
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

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

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        if (isScanningEnabled) {
                            val resultText = processImageProxy(imageProxy, scanner, scannerView)
                            if (resultText.isNotEmpty()) {
                                onQrCodeScanned(resultText)
                            }
                        }
                        imageProxy.close()
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

private fun processImageProxy(
    image: ImageProxy,
    scanner: BarcodeReader,
    previewView: PreviewView
): String {
    return image.use { proxy ->
        try {
            // Get the transformation matrix
//            val matrix = getCorrectionMatrix(image, previewView)

            // Calculate scan area in preview coordinates (center 200dp)
            val scanAreaSize = 200.dp.toPx(previewView.context).toInt()
            // Calculate the center crop region
            val centerX = proxy.width / 2
            val centerY = proxy.height / 2
            val left = (centerX - scanAreaSize / 2).coerceAtLeast(0)
            val top = (centerY - scanAreaSize / 2).coerceAtLeast(0)
            val right = (left + scanAreaSize).coerceAtMost(proxy.width)
            val bottom = (top + scanAreaSize).coerceAtMost(proxy.height)

            val scanArea = Rect(left, top, right, bottom)

            // Get YUV plane data
            val yuvData = image.planes[0].buffer.toByteArray()

            // Create luminance source
            val source = PlanarYUVLuminanceSource(
                yuvData,
                proxy.width,
                proxy.height,
                scanArea.left,
                scanArea.top,
                scanArea.width(),
                scanArea.height(),
                false
            )

            // Configure reader
            val reader = MultiFormatReader().apply {
                setHints(mapOf(
                    com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
                ))
            }

            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            result.text
        } catch (e: Exception) {
            Log.e("QRScanner", "Decoding error: ${e.message}")
            ""
        }
    }
}

// Updated ByteBuffer extension with proper handling
private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    rewind()
    return data
}

// Fixed Dp to pixel conversion
private fun Dp.toPx(context: Context): Float {
    return this.value * context.resources.displayMetrics.density
}

//fun processImageProxy(image: ImageProxy, scanner: BarcodeReader): String {
//    return image.use {
//        scanner.read(it)
//    }.joinToString("\n") { result ->
//        "${result.text}"
//    }
//}