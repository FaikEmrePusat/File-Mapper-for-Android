package com.example.filemapper.ui.canvas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filemapper.R
import com.example.filemapper.data.local.entity.FileNodeType
import com.example.filemapper.ui.state.FileNodeUiState
import kotlin.math.roundToInt

/**
 * Visual representation of a file or folder node on the infinite canvas.
 * 
 * Supports:
 * - Drag to reposition
 * - Long press to show context menu
 * - Click to select (especially in connection selection mode)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileNodeItem(
    nodeState: FileNodeUiState,
    canvasScale: Float,
    isSelected: Boolean = false,
    isConnectionSource: Boolean = false,
    isConnectionTarget: Boolean = false,
    isInSelectionMode: Boolean = false,
    isDisconnectSource: Boolean = false,
    isDisconnectTarget: Boolean = false,
    isSelectedForDisconnect: Boolean = false,
    isDimmed: Boolean = false,
    hasConnections: Boolean = false,
    onDrag: (nodeId: String, newX: Float, newY: Float) -> Unit = { _, _, _ -> },
    onDragEnd: (nodeId: String) -> Unit = {},
    onClick: (FileNodeUiState) -> Unit = {},
    onStartConnectionMode: (nodeId: String) -> Unit = {},
    onStartDisconnectMode: (nodeId: String) -> Unit = {},
    onDelete: (nodeId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    var isDragging by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .offset { IntOffset(nodeState.posX.roundToInt(), nodeState.posY.roundToInt()) }
            .pointerInput(nodeState.id, canvasScale) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Since FileNodeItem is inside a graphicsLayer-scaled Box,
                        // the drag coordinates may already be in canvas space.
                        // Using dragAmount directly without scale division.
                        val newX = nodeState.posX + dragAmount.x
                        val newY = nodeState.posY + dragAmount.y
                        onDrag(nodeState.id, newX, newY)
                    },
                    onDragEnd = {
                        if (isDragging) onDragEnd(nodeState.id)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        NodeCard(
            nodeState = nodeState,
            isSelected = isSelected,
            isDragging = isDragging,
            isConnectionSource = isConnectionSource,
            isConnectionTarget = isConnectionTarget,
            isInSelectionMode = isInSelectionMode,
            onClick = { onClick(nodeState) },
            onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showContextMenu = true
            }
        )
        
        // Context menu dropdown
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = DpOffset(NODE_WIDTH.dp / 2, 0.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Bağlantı Oluştur") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color(0xFF6366F1)
                    )
                },
                onClick = {
                    showContextMenu = false
                    onStartConnectionMode(nodeState.id)
                }
            )
            // Only show disconnect option if node has connections
            if (hasConnections) {
                DropdownMenuItem(
                    text = { Text("Bağlantıyı Kes", color = Color(0xFFFF9800)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onStartDisconnectMode(nodeState.id)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Haritadan Kaldır", color = Color(0xFFEF4444)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                },
                onClick = {
                    showContextMenu = false
                    onDelete(nodeState.id)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NodeCard(
    nodeState: FileNodeUiState,
    isSelected: Boolean,
    isDragging: Boolean,
    isConnectionSource: Boolean,
    isConnectionTarget: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isFolder = nodeState.type == FileNodeType.FOLDER
    
    val cardGradient = if (isFolder) {
        Brush.linearGradient(listOf(Color(0xFF4A90A4), Color(0xFF357A8C)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5)))
    }
    
    val elevation = when {
        isConnectionSource -> 20.dp
        isConnectionTarget -> 18.dp
        isDragging -> 16.dp
        isSelected -> 12.dp
        else -> 6.dp
    }
    
    val borderColor = when {
        isConnectionSource -> Color(0xFFFFD700) // Gold for source
        isConnectionTarget -> Color(0xFF10B981) // Green for target
        isSelected && isInSelectionMode -> Color(0xFF10B981)
        isSelected -> Color(0xFFFFD700)
        isDragging -> Color.White.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    
    val glowColor = when {
        isConnectionSource -> Color(0xFFFFD700)
        isConnectionTarget -> Color(0xFF10B981)
        else -> if (isFolder) Color(0xFF4A90A4) else Color(0xFF6366F1)
    }
    
    Card(
        modifier = Modifier
            .width(NODE_WIDTH.dp)
            .shadow(elevation, RoundedCornerShape(16.dp), ambientColor = glowColor, spotColor = glowColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(2.dp)
        ) {
            if (isSelected || isDragging || isConnectionSource || isConnectionTarget) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(borderColor.copy(alpha = 0.5f), borderColor.copy(alpha = 0.2f))
                            )
                        )
                )
            }
            
            Column(
                modifier = Modifier.width(NODE_WIDTH.dp).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isFolder) R.drawable.ic_folder else R.drawable.ic_file),
                        contentDescription = if (isFolder) "Folder" else "File",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = nodeState.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
                
                // Status indicator
                if (isConnectionSource) {
                    Spacer(Modifier.height(4.dp))
                    Text("KAYNAK", fontSize = 8.sp, color = Color(0xFFFFD700))
                } else if (isConnectionTarget) {
                    Spacer(Modifier.height(4.dp))
                    Text("HEDEF", fontSize = 8.sp, color = Color(0xFF10B981))
                }
            }
        }
    }
}

internal const val NODE_WIDTH = 100f
internal const val NODE_HEIGHT = 110f
