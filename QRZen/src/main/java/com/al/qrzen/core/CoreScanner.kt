package com.al.qrzen.core

import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader
import kotlin.use

class CoreScanner {
    fun processImageProxy(image: ImageProxy, scanner: BarcodeReader): String {
        return image.use {
            scanner.read(it)
        }.joinToString("\n") { result ->
            "${result.text}"
        }
    }
}