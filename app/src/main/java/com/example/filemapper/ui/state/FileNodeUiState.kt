package com.example.filemapper.ui.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.example.filemapper.data.local.entity.FileNode
import com.example.filemapper.data.local.entity.FileNodeType

/**
 * UI State wrapper for FileNode.
 * 
 * Uses MutableState for position (offsetX/offsetY) to enable
 * INSTANT UI updates during drag operations.
 * 
 * Connection lines read position from this class, so when the user
 * drags a node, the connected lines move IMMEDIATELY without any delay.
 * 
 * Database persistence only happens on drag END, not during drag.
 */
@Stable
class FileNodeUiState(
    val id: String,
    val displayName: String,
    val type: FileNodeType,
    val parentPath: String?,
    initialPosX: Float,
    initialPosY: Float
) {
    /** 
     * Current X position - MutableState for instant recomposition.
     * Updated DURING drag for real-time UI updates.
     */
    var posX by mutableFloatStateOf(initialPosX)
    
    /**
     * Current Y position - MutableState for instant recomposition.
     * Updated DURING drag for real-time UI updates.
     */
    var posY by mutableFloatStateOf(initialPosY)
    
    /**
     * Update position instantly (called DURING drag).
     * This triggers immediate recomposition of this node and any connected lines.
     */
    fun updatePosition(newX: Float, newY: Float) {
        posX = newX
        posY = newY
    }
    
    /**
     * Convert back to Room entity for database persistence.
     */
    fun toFileNode(): FileNode {
        return FileNode(
            id = id,
            displayName = displayName,
            posX = posX,
            posY = posY,
            parentPath = parentPath,
            type = type
        )
    }
    
    companion object {
        /**
         * Create UI state from Room entity.
         */
        fun fromFileNode(node: FileNode): FileNodeUiState {
            return FileNodeUiState(
                id = node.id,
                displayName = node.displayName,
                type = node.type,
                parentPath = node.parentPath,
                initialPosX = node.posX,
                initialPosY = node.posY
            )
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileNodeUiState) return false
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
}
