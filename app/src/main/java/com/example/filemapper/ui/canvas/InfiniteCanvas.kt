package com.example.filemapper.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.example.filemapper.ui.theme.GridDot
import com.example.filemapper.ui.theme.Navy

/**
 * An infinite 2D canvas with pan and zoom capabilities.
 * Features a dynamic dot grid background that moves with transformations.
 * 
 * GESTURE PRIORITY:
 * - If user touches a Node: Node handles the gesture (drag), Canvas ignores
 * - If user touches empty space: Canvas handles pan/zoom
 * - Pinch zoom always works (uses 2+ fingers)
 * 
 * @param modifier Modifier to apply to the canvas container
 * @param state The canvas state containing scale and offset
 * @param backgroundColor Background color of the canvas
 * @param gridColor Color of the dot grid
 * @param gridSpacing Base spacing between grid dots (in canvas coordinates)
 * @param dotRadius Radius of each grid dot
 * @param content Content to display on the canvas, rendered with transformations applied
 */
@Composable
fun InfiniteCanvas(
    modifier: Modifier = Modifier,
    state: CanvasState = rememberCanvasState(),
    backgroundColor: Color = Navy,
    gridColor: Color = GridDot,
    gridSpacing: Float = 40f,
    dotRadius: Float = 2f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Use Final pass to let children (nodes) consume events first
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Wait for first touch
                    val firstDown = awaitFirstDown(pass = PointerEventPass.Final)
                    
                    // If the event was already consumed by a child (Node), skip canvas gesture
                    if (firstDown.isConsumed) {
                        return@awaitEachGesture
                    }
                    
                    // Track for pan/zoom
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        
                        // Check if any child consumed this event
                        val anyConsumed = event.changes.any { it.isConsumed }
                        if (anyConsumed) {
                            // Child is handling, don't interfere
                            break
                        }
                        
                        // Calculate gesture values from unconsumed events
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        
                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange
                            val centroidSize = event.calculateCentroid(useCurrent = false)
                            val zoomMotion = kotlin.math.abs(1 - zoom) * centroidSize.getDistance()
                            val panMotion = pan.getDistance()
                            
                            if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                pastTouchSlop = true
                            }
                        }
                        
                        if (pastTouchSlop) {
                            val centroid = event.calculateCentroid(useCurrent = true)
                            
                            // Apply zoom
                            if (zoomChange != 1f) {
                                state.zoom(zoomChange, centroid)
                            }
                            
                            // Apply pan
                            if (panChange != Offset.Zero) {
                                state.pan(panChange)
                            }
                            
                            // Consume changes to prevent parent handlers
                            event.changes.forEach { change ->
                                if (change.positionChanged()) {
                                    change.consume()
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        // Draw the dot grid background
        DotGridBackground(
            state = state,
            gridColor = gridColor,
            gridSpacing = gridSpacing,
            dotRadius = dotRadius
        )
        
        // Transformed content container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                    // Set transform origin to top-left for consistent positioning
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                },
            content = content
        )
    }
}

/**
 * Draws a dynamic dot grid that responds to canvas pan and zoom.
 */
@Composable
private fun DotGridBackground(
    state: CanvasState,
    gridColor: Color,
    gridSpacing: Float,
    dotRadius: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scaledSpacing = gridSpacing * state.scale
        
        // Don't draw grid if spacing is too small (too zoomed out)
        if (scaledSpacing < 10f) return@Canvas
        
        // Calculate the visible area in canvas coordinates
        val offsetX = state.offset.x
        val offsetY = state.offset.y
        
        // Calculate grid start positions (aligned to grid)
        val startX = offsetX % scaledSpacing
        val startY = offsetY % scaledSpacing
        
        // Calculate number of dots to draw
        val dotsHorizontal = (size.width / scaledSpacing).toInt() + 2
        val dotsVertical = (size.height / scaledSpacing).toInt() + 2
        
        // Scale dot radius with zoom, but with limits
        val scaledDotRadius = (dotRadius * state.scale).coerceIn(1f, 4f)
        
        // Draw dots
        for (i in 0 until dotsHorizontal) {
            for (j in 0 until dotsVertical) {
                val x = startX + i * scaledSpacing
                val y = startY + j * scaledSpacing
                
                // Only draw if within visible bounds
                if (x >= -scaledDotRadius && x <= size.width + scaledDotRadius &&
                    y >= -scaledDotRadius && y <= size.height + scaledDotRadius
                ) {
                    drawCircle(
                        color = gridColor,
                        radius = scaledDotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

