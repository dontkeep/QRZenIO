package com.al.qrzen.scanner

import android.view.MotionEvent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.al.qrzen.core.CoreScanner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

@Composable
fun BorderScanner(
    modifier: Modifier = Modifier,
    isScanningEnabled: Boolean,
    onQrCodeScanned: (String) -> Unit,
    isFlashEnabled: Boolean,
    isZoomEnabled: Boolean,
    isAutoFocusEnabled: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scanner = remember { BarcodeReader() }

    var flashEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    Box(modifier = modifier.fillMaxSize()) {
        var previewView: PreviewView? = null
        val scanningState by rememberUpdatedState(newValue = isScanningEnabled)

        val processor = remember {
            CoreScanner(scanner) { result ->
                onQrCodeScanned(result)
            }.apply {
                this.isScanningEnabled = { scanningState }
                this.getPreviewView = { previewView }
            }
        }

        AndroidView(
            factory = { ctx ->
                previewView = PreviewView(ctx).apply {
                    if (isAutoFocusEnabled) {
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                val factory = this.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    .disableAutoCancel()
                                    .build()
                                camera?.cameraControl?.startFocusAndMetering(action)

                                // Inform accessibility services and standard tap handling
                                view.performClick()
                            }
                            true
                        }
                    }
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView!!.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx), processor)
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

                        // Listen for max zoom ratio
                        camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                            if (zoomRatio > zoomState.maxZoomRatio) {
                                zoomRatio = zoomState.maxZoomRatio
                            }
                        }

                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView!!
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay with transparent scanning area
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

        // White border
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
        )

        // Flash toggle
        if (isFlashEnabled) {
            IconButton(
                onClick = {
                    flashEnabled = !flashEnabled
                    camera?.cameraControl?.enableTorch(flashEnabled)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isZoomEnabled) 96.dp else 48.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(id = if (flashEnabled) R.drawable.ic_flash_filled else R.drawable.ic_flash_outline),
                    contentDescription = "Flash Toggle",
                    tint = Color.White
                )
            }
        }

        // Zoom slider
        if (isZoomEnabled) {
            Slider(
                value = zoomRatio,
                onValueChange = {
                    zoomRatio = it
                    camera?.cameraControl?.setZoomRatio(it)
                },
                valueRange = 1f..(camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 4f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )
        }
    }
}

