package com.example.filemapper.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs

/**
 * Draws a curved Bezier connection line between two points.
 * Uses cubic Bezier curves for a modern, smooth appearance.
 *
 * @param startPoint Starting coordinates (typically center of source node)
 * @param endPoint Ending coordinates (typically center of target node)
 * @param color Line color
 * @param strokeWidth Width of the line
 * @param isDashed Whether to draw a dashed line (for temporary connections)
 * @param modifier Modifier for the canvas
 */
@Composable
fun ConnectionLine(
    startPoint: Offset,
    endPoint: Offset,
    color: Color = Color(0xFF8B5CF6),
    strokeWidth: Float = 3f,
    isDashed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val path = createBezierPath(startPoint, endPoint)
        
        val pathEffect = if (isDashed) {
            PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
        } else {
            null
        }
        
        // Draw glow effect (behind main line)
        if (!isDashed) {
            drawPath(
                path = path,
                color = color.copy(alpha = 0.3f),
                style = Stroke(
                    width = strokeWidth + 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        
        // Draw main line
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = pathEffect
            )
        )
    }
}

/**
 * Draws a gradient connection line with animated appearance.
 */
@Composable
fun GradientConnectionLine(
    startPoint: Offset,
    endPoint: Offset,
    startColor: Color = Color(0xFF6366F1),
    endColor: Color = Color(0xFF8B5CF6),
    strokeWidth: Float = 3f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val path = createBezierPath(startPoint, endPoint)
        
        val gradient = Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = startPoint,
            end = endPoint
        )
        
        // Draw glow
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    startColor.copy(alpha = 0.3f),
                    endColor.copy(alpha = 0.3f)
                ),
                start = startPoint,
                end = endPoint
            ),
            style = Stroke(
                width = strokeWidth + 6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Draw main line
        drawPath(
            path = path,
            brush = gradient,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Draws a temporary connection line while in connection mode.
 * Uses dashed style to indicate it's not yet confirmed.
 */
@Composable
fun TemporaryConnectionLine(
    startPoint: Offset,
    endPoint: Offset,
    modifier: Modifier = Modifier
) {
    ConnectionLine(
        startPoint = startPoint,
        endPoint = endPoint,
        color = Color(0xFFFFD700), // Gold color for temporary
        strokeWidth = 2.5f,
        isDashed = true,
        modifier = modifier
    )
}

/**
 * Creates a cubic Bezier path between two points.
 * The curve adapts based on the relative positions of start and end.
 */
private fun createBezierPath(start: Offset, end: Offset): Path {
    val path = Path()
    path.moveTo(start.x, start.y)
    
    // Calculate control points for smooth curve
    val deltaX = end.x - start.x
    val deltaY = end.y - start.y
    
    // Determine curve direction based on relative positions
    val controlOffset = minOf(abs(deltaX), abs(deltaY)) * 0.5f + 50f
    
    // For mostly horizontal connections
    if (abs(deltaX) > abs(deltaY)) {
        val controlX1 = start.x + deltaX * 0.4f
        val controlY1 = start.y
        val controlX2 = end.x - deltaX * 0.4f
        val controlY2 = end.y
        
        path.cubicTo(
            controlX1, controlY1,
            controlX2, controlY2,
            end.x, end.y
        )
    } else {
        // For mostly vertical connections
        val controlX1 = start.x
        val controlY1 = start.y + deltaY * 0.4f
        val controlX2 = end.x
        val controlY2 = end.y - deltaY * 0.4f
        
        path.cubicTo(
            controlX1, controlY1,
            controlX2, controlY2,
            end.x, end.y
        )
    }
    
    return path
}

/**
 * Helper to calculate the center point of a node for connection attachment.
 */
fun calculateNodeCenter(posX: Float, posY: Float, nodeWidth: Float = 100f, nodeHeight: Float = 110f): Offset {
    return Offset(
        x = posX + nodeWidth / 2f,
        y = posY + nodeHeight / 2f
    )
}
