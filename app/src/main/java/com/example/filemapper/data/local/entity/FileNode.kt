package com.example.filemapper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Enum representing the type of file node
 */
enum class FileNodeType {
    FOLDER,
    FILE
}

/**
 * Room Entity representing a file or folder node on the infinite canvas.
 * Stores position coordinates for spatial layout.
 */
@Entity(tableName = "file_nodes")
data class FileNode(
    @PrimaryKey
    val id: String,                    // File path as unique identifier
    val displayName: String,           // Display name of the file/folder
    val posX: Float,                   // X coordinate on canvas
    val posY: Float,                   // Y coordinate on canvas
    val parentPath: String? = null,    // Parent folder path (nullable for root)
    val type: FileNodeType             // Type: FOLDER or FILE
)
