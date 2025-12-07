package com.example.filemapper.domain.repository

import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.data.local.entity.FileNode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for file operations.
 * Defines the contract for data layer operations.
 */
interface FileRepository {
    
    // ==================== FileNode Operations ====================
    
    /**
     * Sync a directory - scans real files and adds missing ones to database.
     * @param path The directory path to sync
     * @return List of newly added FileNodes
     */
    suspend fun syncDirectory(path: String): List<FileNode>
    
    /**
     * Get all nodes as a Flow for reactive updates.
     */
    fun observeAllNodes(): Flow<List<FileNode>>
    
    /**
     * Get nodes under a specific parent folder.
     */
    fun observeNodesByParent(parentPath: String): Flow<List<FileNode>>
    
    /**
     * Get root nodes (no parent).
     */
    fun observeRootNodes(): Flow<List<FileNode>>
    
    /**
     * Get a specific node by ID.
     */
    suspend fun getNodeById(nodeId: String): FileNode?
    
    /**
     * Update the position of a node on the canvas.
     */
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float)
    
    /**
     * Insert or update a node.
     */
    suspend fun insertNode(node: FileNode)
    
    /**
     * Delete a node.
     */
    suspend fun deleteNode(nodeId: String)
    
    /**
     * Delete all nodes.
     */
    suspend fun deleteAllNodes()
    
    // ==================== FileConnection Operations ====================
    
    /**
     * Get all connections as a Flow.
     */
    fun observeAllConnections(): Flow<List<FileConnection>>
    
    /**
     * Get connections for a specific node.
     */
    fun observeConnectionsForNode(nodePath: String): Flow<List<FileConnection>>
    
    /**
     * Create a connection between two nodes.
     */
    suspend fun createConnection(sourcePath: String, targetPath: String)
    
    /**
     * Delete a connection.
     */
    suspend fun deleteConnection(connectionId: Long)
    
    /**
     * Delete all connections for a node.
     */
    suspend fun deleteConnectionsForNode(nodePath: String)
    
    /**
     * Delete connection between two specific nodes (bidirectional).
     */
    suspend fun deleteConnectionBetween(path1: String, path2: String)
    
    /**
     * Get all node IDs connected to a specific node.
     */
    suspend fun getConnectedNodeIds(nodePath: String): List<String>
}
