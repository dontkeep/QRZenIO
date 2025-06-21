package com.al.qrzen.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ZoomSlider(
    zoomRatio: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = zoomRatio,
        onValueChange = onZoomChanged,
        valueRange = 1f..5f,
        modifier = modifier
            .padding(horizontal = 64.dp)
    )
}