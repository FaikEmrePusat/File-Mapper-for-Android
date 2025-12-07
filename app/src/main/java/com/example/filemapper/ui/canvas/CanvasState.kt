package com.example.filemapper.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for InfiniteCanvas.
 * Manages zoom (scale) and pan (offset) transformations.
 */
@Stable
class CanvasState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
) {
    /** Current zoom level. Clamped between MIN_SCALE and MAX_SCALE. */
    var scale by mutableFloatStateOf(initialScale)
        private set
    
    /** Current pan offset in screen coordinates. */
    var offset by mutableStateOf(Offset(initialOffsetX, initialOffsetY))
        private set
    
    companion object {
        const val MIN_SCALE = 0.2f
        const val MAX_SCALE = 4f
        const val DEFAULT_SCALE = 1f
        
        /**
         * Saver for persisting CanvasState across configuration changes.
         */
        val Saver: Saver<CanvasState, *> = listSaver(
            save = { listOf(it.scale, it.offset.x, it.offset.y) },
            restore = { CanvasState(it[0], it[1], it[2]) }
        )
    }
    
    /**
     * Apply zoom transformation centered on a focal point.
     */
    fun zoom(zoomChange: Float, centroid: Offset) {
        val oldScale = scale
        val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        
        if (newScale != oldScale) {
            // Adjust offset to keep the centroid point stationary
            val scaleDelta = newScale / oldScale
            offset = Offset(
                x = centroid.x - (centroid.x - offset.x) * scaleDelta,
                y = centroid.y - (centroid.y - offset.y) * scaleDelta
            )
            scale = newScale
        }
    }
    
    /**
     * Apply pan transformation.
     */
    fun pan(panChange: Offset) {
        offset = Offset(
            x = offset.x + panChange.x,
            y = offset.y + panChange.y
        )
    }
    
    /**
     * Reset to default state.
     */
    fun reset() {
        scale = DEFAULT_SCALE
        offset = Offset.Zero
    }
    
    /**
     * Convert screen coordinates to canvas coordinates.
     */
    fun screenToCanvas(screenPoint: Offset): Offset {
        return Offset(
            x = (screenPoint.x - offset.x) / scale,
            y = (screenPoint.y - offset.y) / scale
        )
    }
    
    /**
     * Convert screen coordinates to canvas coordinates.
     * Convenience overload that takes separate x and y parameters.
     */
    fun screenToCanvas(screenX: Float, screenY: Float): Offset {
        return screenToCanvas(Offset(screenX, screenY))
    }
    
    /**
     * Convert canvas coordinates to screen coordinates.
     */
    fun canvasToScreen(canvasPoint: Offset): Offset {
        return Offset(
            x = canvasPoint.x * scale + offset.x,
            y = canvasPoint.y * scale + offset.y
        )
    }
}

/**
 * Remember and save CanvasState across recompositions and configuration changes.
 */
@Composable
fun rememberCanvasState(
    initialScale: Float = 1f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f
): CanvasState {
    return rememberSaveable(saver = CanvasState.Saver) {
        CanvasState(initialScale, initialOffsetX, initialOffsetY)
    }
}
