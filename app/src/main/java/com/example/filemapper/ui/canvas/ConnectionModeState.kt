package com.example.filemapper.ui.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder for connection creation mode.
 * Manages the temporary connection line while user is dragging.
 */
@Stable
class ConnectionModeState {
    /** Whether connection mode is currently active */
    var isActive by mutableStateOf(false)
        private set
    
    /** The source node ID that initiated the connection */
    var sourceNodeId: String? by mutableStateOf(null)
        private set
    
    /** The starting point (center of source node) in canvas coordinates */
    var startPoint by mutableStateOf(Offset.Zero)
        private set
    
    /** The current end point (follows finger) in canvas coordinates */
    var currentEndPoint by mutableStateOf(Offset.Zero)
        private set
    
    /** The target node ID if finger is over a valid target */
    var targetNodeId: String? by mutableStateOf(null)
        private set
    
    /**
     * Start connection mode from a source node.
     */
    fun startConnection(nodeId: String, nodeCenter: Offset) {
        isActive = true
        sourceNodeId = nodeId
        startPoint = nodeCenter
        currentEndPoint = nodeCenter
        targetNodeId = null
    }
    
    /**
     * Update the current drag position.
     */
    fun updateDragPosition(position: Offset) {
        if (isActive) {
            currentEndPoint = position
        }
    }
    
    /**
     * Set the current target node (when hovering over a valid target).
     */
    fun setTarget(nodeId: String?, nodeCenter: Offset? = null) {
        targetNodeId = nodeId
        if (nodeCenter != null && nodeId != null) {
            // Snap to target center for visual feedback
            currentEndPoint = nodeCenter
        }
    }
    
    /**
     * Cancel connection mode without creating a connection.
     */
    fun cancel() {
        isActive = false
        sourceNodeId = null
        startPoint = Offset.Zero
        currentEndPoint = Offset.Zero
        targetNodeId = null
    }
    
    /**
     * Complete the connection and return the source/target pair if valid.
     * Returns null if no valid target was set.
     */
    fun complete(): Pair<String, String>? {
        val source = sourceNodeId
        val target = targetNodeId
        
        // Reset state
        cancel()
        
        // Return pair only if both source and target are valid and different
        return if (source != null && target != null && source != target) {
            Pair(source, target)
        } else {
            null
        }
    }
}

/**
 * Remember connection mode state.
 */
@Composable
fun rememberConnectionModeState(): ConnectionModeState {
    return androidx.compose.runtime.remember { ConnectionModeState() }
}
