package com.al.qrzen.core

import android.content.res.Resources
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zxingcpp.BarcodeReader
import java.io.ByteArrayOutputStream

class CoreBorderQRScanner(
    private val scanner: BarcodeReader,
    private val onScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // These will be set by the Composable
    var isScanningEnabled: () -> Boolean = { true }
    var getPreviewView: () -> PreviewView? = { null }

    // Extension function to convert dp to px
    private fun Dp.toPx(): Float = this.value * Resources.getSystem().displayMetrics.density

    // Fixed scan area size in pixels (matches your UI border)
    private val scanAreaSizePx = 350.dp.toPx().toInt()

    override fun analyze(image: ImageProxy) {
        Log.d("QRScanner", "Analyzing image: ${image.width}x${image.height}")
        if (!isScanningEnabled()) {
            image.close()
            return
        }

        val previewView = getPreviewView() ?: run {
            image.close()
            return
        }

        try {
            // 1. Calculate the ACTUAL visible preview rect in the PreviewView
            val previewRect = calculateVisiblePreviewRect(previewView)
            if (previewRect.width() <= 0 || previewRect.height() <= 0) {
                image.close()
                return
            }

            // 2. Calculate the scan area in preview coordinates (center)
            val scanRect = Rect(
                (previewRect.width() - scanAreaSizePx) / 2,
                (previewRect.height() - scanAreaSizePx) / 2,
                (previewRect.width() + scanAreaSizePx) / 2,
                (previewRect.height() + scanAreaSizePx) / 2
            )

            // 3. Map to image coordinates with proper scaling
            val scaleX = image.width.toFloat() / previewRect.width()
            val scaleY = image.height.toFloat() / previewRect.height()

            val imageRect = Rect(
                (scanRect.left * scaleX).toInt().coerceAtLeast(0),
                (scanRect.top * scaleY).toInt().coerceAtLeast(0),
                (scanRect.right * scaleX).toInt().coerceAtMost(image.width),
                (scanRect.bottom * scaleY).toInt().coerceAtMost(image.height)
            )

            // 4. Crop and scan ONLY this area
            val croppedBitmap = image.toCroppedBitmap(imageRect) ?: return
            val result = scanner.read(croppedBitmap).firstOrNull()?.text ?: ""
            if (result.isNotEmpty()) onScanned(result)
            croppedBitmap.recycle()
        } catch (e: Exception) {
            Log.e("QRScanner", "Scan error", e)
        } finally {
            image.close()
        }
    }

    private fun calculateVisiblePreviewRect(previewView: PreviewView): Rect {
        // Use standard 16:9 aspect ratio for most phone cameras
        val previewRatio = 16f / 9f

        return when (previewView.scaleType) {
            PreviewView.ScaleType.FILL_CENTER -> {
                val viewRatio = previewView.width.toFloat() / previewView.height

                if (viewRatio > previewRatio) {
                    // Preview is taller than view
                    val previewHeight = previewView.width / previewRatio
                    Rect(0, ((previewView.height - previewHeight) / 2).toInt(),
                        previewView.width, ((previewView.height + previewHeight) / 2).toInt())
                } else {
                    // Preview is wider than view
                    val previewWidth = previewView.height * previewRatio
                    Rect(((previewView.width - previewWidth) / 2).toInt(), 0,
                        ((previewView.width + previewWidth) / 2).toInt(), previewView.height)
                }
            }
            else -> Rect(0, 0, previewView.width, previewView.height)
        }
    }

    private fun ImageProxy.toCroppedBitmap(rect: Rect): Bitmap? {
        return try {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            // Get YUV data
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            // Create YUV image and crop
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream().use { stream ->
                yuvImage.compressToJpeg(rect, 90, stream)
                stream.toByteArray()
            }

            BitmapFactory.decodeByteArray(out, 0, out.size)
        } catch (e: Exception) {
            null
        }
    }
}