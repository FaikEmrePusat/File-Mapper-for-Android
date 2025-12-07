package com.example.filemapper.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.data.local.entity.FileNode
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FileNode and FileConnection entities.
 * Provides CRUD operations and reactive queries using Flow.
 */
@Dao
interface FileNodeDao {
    
    // ==================== FileNode CRUD ====================
    
    /**
     * Insert a new file node. Replaces on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: FileNode)
    
    /**
     * Insert multiple file nodes. Replaces on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<FileNode>)
    
    /**
     * Update an existing file node.
     */
    @Update
    suspend fun updateNode(node: FileNode)
    
    /**
     * Delete a file node.
     */
    @Delete
    suspend fun deleteNode(node: FileNode)
    
    /**
     * Delete a file node by its ID (path).
     */
    @Query("DELETE FROM file_nodes WHERE id = :nodeId")
    suspend fun deleteNodeById(nodeId: String)
    
    /**
     * Get a file node by its ID (path).
     */
    @Query("SELECT * FROM file_nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: String): FileNode?
    
    /**
     * Get a file node by its ID as a Flow for reactive updates.
     */
    @Query("SELECT * FROM file_nodes WHERE id = :nodeId")
    fun observeNodeById(nodeId: String): Flow<FileNode?>
    
    /**
     * Get all file nodes as a Flow.
     */
    @Query("SELECT * FROM file_nodes")
    fun observeAllNodes(): Flow<List<FileNode>>
    
    /**
     * Get all file nodes under a specific parent folder as a Flow.
     */
    @Query("SELECT * FROM file_nodes WHERE parentPath = :parentPath")
    fun observeNodesByParent(parentPath: String): Flow<List<FileNode>>
    
    /**
     * Get all root nodes (nodes with null parentPath) as a Flow.
     */
    @Query("SELECT * FROM file_nodes WHERE parentPath IS NULL")
    fun observeRootNodes(): Flow<List<FileNode>>
    
    /**
     * Update the position of a file node.
     */
    @Query("UPDATE file_nodes SET posX = :x, posY = :y WHERE id = :nodeId")
    suspend fun updateNodePosition(nodeId: String, x: Float, y: Float)
    
    /**
     * Get all nodes (non-Flow, for one-time queries).
     */
    @Query("SELECT * FROM file_nodes")
    suspend fun getAllNodes(): List<FileNode>
    
    /**
     * Delete all nodes.
     */
    @Query("DELETE FROM file_nodes")
    suspend fun deleteAllNodes()
    
    /**
     * Get only node IDs for a specific parent path (optimized query for sync).
     * Returns only IDs to avoid loading full FileNode objects.
     */
    @Query("SELECT id FROM file_nodes WHERE parentPath = :parentPath")
    suspend fun getNodeIdsByParent(parentPath: String): List<String>
    
    // ==================== FileConnection CRUD ====================
    
    /**
     * Insert a new connection.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: FileConnection)
    
    /**
     * Delete a connection.
     */
    @Delete
    suspend fun deleteConnection(connection: FileConnection)
    
    /**
     * Delete a connection by its ID.
     */
    @Query("DELETE FROM file_connections WHERE id = :connectionId")
    suspend fun deleteConnectionById(connectionId: Long)
    
    /**
     * Get all connections as a Flow.
     */
    @Query("SELECT * FROM file_connections")
    fun observeAllConnections(): Flow<List<FileConnection>>
    
    /**
     * Get all connections where a specific node is the source.
     */
    @Query("SELECT * FROM file_connections WHERE sourcePath = :nodePath")
    fun observeConnectionsFromNode(nodePath: String): Flow<List<FileConnection>>
    
    /**
     * Get all connections where a specific node is the target.
     */
    @Query("SELECT * FROM file_connections WHERE targetPath = :nodePath")
    fun observeConnectionsToNode(nodePath: String): Flow<List<FileConnection>>
    
    /**
     * Get all connections involving a specific node (as source or target).
     */
    @Query("SELECT * FROM file_connections WHERE sourcePath = :nodePath OR targetPath = :nodePath")
    fun observeConnectionsForNode(nodePath: String): Flow<List<FileConnection>>
    
    /**
     * Delete all connections involving a specific node.
     */
    @Query("DELETE FROM file_connections WHERE sourcePath = :nodePath OR targetPath = :nodePath")
    suspend fun deleteConnectionsForNode(nodePath: String)
    
    /**
     * Delete all connections.
     */
    @Query("DELETE FROM file_connections")
    suspend fun deleteAllConnections()
    
    /**
     * Check if a connection exists between two nodes.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM file_connections WHERE sourcePath = :sourcePath AND targetPath = :targetPath)")
    suspend fun connectionExists(sourcePath: String, targetPath: String): Boolean
    
    /**
     * Delete connection between two specific nodes (bidirectional).
     */
    @Query("DELETE FROM file_connections WHERE (sourcePath = :path1 AND targetPath = :path2) OR (sourcePath = :path2 AND targetPath = :path1)")
    suspend fun deleteConnectionBetween(path1: String, path2: String)
    
    /**
     * Get all node IDs connected to a specific node.
     */
    @Query("SELECT targetPath FROM file_connections WHERE sourcePath = :nodePath UNION SELECT sourcePath FROM file_connections WHERE targetPath = :nodePath")
    suspend fun getConnectedNodeIds(nodePath: String): List<String>
}
