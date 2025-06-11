package com.al.qrzen.scanner

import android.util.Size
import android.view.MotionEvent
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
    isZoomEnabled: Boolean,
    isAutoFocusEnabled: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeReader() }

    var flashEnabled by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    Box(modifier = modifier.fillMaxSize()) {
        var previewView: PreviewView? = null

        val scanningState by rememberUpdatedState(newValue = isScanningEnabled)

        val processor = remember {
            CoreScanner(scanner) { qrText ->
                onQrCodeScanned(qrText)
            }.apply {
                this.isScanningEnabled = { scanningState }
                this.getPreviewView = { previewView }
            }
        }

        var zoomRatioState by remember { mutableFloatStateOf(1f) }
        var maxZoomRatio by remember { mutableFloatStateOf(4f) }

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
                        .setTargetResolution(Size(1280, 720))
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
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView!!
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isFlashEnabled || isZoomEnabled) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isZoomEnabled) {
                    var sliderValue by remember { mutableFloatStateOf(zoomRatioState) }

                    LaunchedEffect(sliderValue) {
                        snapshotFlow { sliderValue }
                            .debounce(25)
                            .collect { zoom ->
                                camera?.cameraControl?.setZoomRatio(zoom)
                            }
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 1f..maxZoomRatio,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                    )
                }

                if (isFlashEnabled) {
                    IconButton(
                        onClick = {
                            flashEnabled = !flashEnabled
                            camera?.cameraControl?.enableTorch(flashEnabled)
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (flashEnabled) R.drawable.ic_flash_filled else R.drawable.ic_flash_outline
                            ),
                            contentDescription = "Flash Toggle",
                            tint = Color.White
                        )
                    }
                }
            }
        }

    }
}