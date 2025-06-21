package com.al.qrzen.core

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import zxingcpp.BarcodeReader
import java.nio.ByteBuffer

class CoreBorderQRScanner(
    private val scanner: BarcodeReader,
    private val onScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    var isScanningEnabled: () -> Boolean = { true }
    var getPreviewView: () -> PreviewView? = { null }

    override fun analyze(image: ImageProxy) {
        Log.d(
            "QRScanner",
            "Analyzing image: ${image.width}x${image.height}, format: ${image.format}"
        )
        if (!isScanningEnabled()) {
            image.close()
            return
        }

        val preview = getPreviewView()
        val result = if (preview != null) {
            borderedProcessImageProxy(image, preview)
        } else {
            Log.e("QRScanner", "PreviewView is null")
            ""
        }

        if (result.isNotEmpty()) {
            onScanned(result)
        }
        image.close()
    }


    private fun borderedProcessImageProxy(
        image: ImageProxy,
        previewView: PreviewView
    ): String {
        return image.use { proxy ->
            try {
                val scanAreaSize = 350.dp.toPx(previewView.context).toInt()
                val centerX = proxy.width / 2
                val centerY = proxy.height / 2
                val left = (centerX - scanAreaSize / 2).coerceAtLeast(0)
                val top = (centerY - scanAreaSize / 2).coerceAtLeast(0)
                val right = (left + scanAreaSize).coerceAtMost(proxy.width)
                val bottom = (top + scanAreaSize).coerceAtMost(proxy.height)

                val scanArea = Rect(left, top, right, bottom)

                val yuvData = image.planes[0].buffer.toByteArray()

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

                val reader = MultiFormatReader().apply {
                    setHints(
                        mapOf(
                            DecodeHintType.POSSIBLE_FORMATS to BarcodeFormat.entries
                        )
                    )
                }

                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
                result.text
            } catch (e: Exception) {
                Log.e("QRScanner", "Decoding error: ${e.message}")
                ""
            }
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        rewind()
        return data
    }

    private fun Dp.toPx(context: Context): Float {
        return this.value * context.resources.displayMetrics.density
    }
}