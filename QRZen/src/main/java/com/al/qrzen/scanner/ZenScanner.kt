package com.al.qrzen.scanner

import android.util.Size
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import zxingcpp.BarcodeReader
import com.al.qrzen.R
import com.al.qrzen.core.CoreScanner
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun ZenScannerScreen(
    modifier: Modifier = Modifier,
    isScanningEnabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
    isFlashEnabled: Boolean,
    isZoomEnabled: Boolean = false,
    isTapToFocusEnabled: Boolean = false
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scanner = BarcodeReader()
    var flashEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    // Delay logic: store last scan time
    var lastScanTimeMillis by remember { mutableLongStateOf(0L) }

    val previewView = remember { PreviewView(context) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) {
                        val now = System.currentTimeMillis()
                        if (isScanningEnabled && now - lastScanTimeMillis > 300) {
                            val resultText = processImageProxy(it, scanner)
                            if (resultText.isNotEmpty()) {
                                lastScanTimeMillis = now
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
                if (isTapToFocusEnabled) {
                    previewView.viewTreeObserver.addOnGlobalLayoutListener(
                        object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                previewView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                                previewView.setOnTouchListener { view, event ->
                                    if (event.action == MotionEvent.ACTION_DOWN) {
                                        val factory = previewView.meteringPointFactory
                                        val point = factory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(point).build()
                                        camera?.cameraControl?.startFocusAndMetering(action)
                                        view.performClick()
                                        return@setOnTouchListener true
                                    }
                                    false
                                }
                            }
                        }
                    )
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isZoomEnabled) {
            Slider(
                value = zoomRatio,
                onValueChange = {
                    zoomRatio = it
                    camera?.cameraControl?.setZoomRatio(it)
                },
                valueRange = 1f..5f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 128.dp, start = 64.dp, end = 64.dp)
            )
        }

        if (isFlashEnabled) {
            IconButton(
                onClick = {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 52.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = if (flashEnabled) R.drawable.torchiconon else R.drawable.torchicon),
                    contentDescription = "Flash Toggle",
                    tint = Color.White
                )
            }
        }
    }
}

fun processImageProxy(image: ImageProxy, scanner: BarcodeReader): String {
    return image.use {
        scanner.read(it)
    }.joinToString("\n") { result ->
        "${result.text}"
    }
}