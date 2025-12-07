package com.example.filemapper.ui.main

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.data.local.entity.FileNode
import com.example.filemapper.data.local.entity.FileNodeType
import com.example.filemapper.domain.repository.FileRepository
import com.example.filemapper.ui.state.FileNodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main screen.
 * 
 * Uses FileNodeUiState with MutableState<Float> for positions.
 * This enables INSTANT UI updates during drag - connection lines
 * move in real-time as the node is dragged.
 * 
 * Database persistence only happens on drag END.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {
    
    // ==================== UI STATE (MutableState for instant updates) ====================
    
    /**
     * Map of node ID -> FileNodeUiState.
     * FileNodeUiState uses MutableState for posX/posY, enabling instant recomposition.
     * Connection lines read from these states directly.
     */
    private val _nodeStates = mutableStateMapOf<String, FileNodeUiState>()
    val nodeStates: SnapshotStateMap<String, FileNodeUiState> = _nodeStates
    
    /**
     * Live list of connections from database.
     */
    private val _connections = mutableStateMapOf<Long, FileConnection>()
    val connections: SnapshotStateMap<Long, FileConnection> = _connections
    
    // ==================== UI State Flags ====================
    
    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Collect nodes from database and convert to UI state
        viewModelScope.launch {
            fileRepository.observeAllNodes().collect { dbNodes ->
                syncNodesFromDatabase(dbNodes)
            }
        }
        
        // Collect connections from database
        viewModelScope.launch {
            fileRepository.observeAllConnections().collect { dbConnections ->
                _connections.clear()
                dbConnections.forEach { conn ->
                    _connections[conn.id] = conn
                }
            }
        }
    }
    
    /**
     * Sync database nodes with UI state.
     * Creates FileNodeUiState wrappers for new nodes.
     * Preserves existing UI state positions (important during drag).
     */
    private fun syncNodesFromDatabase(dbNodes: List<FileNode>) {
        val dbNodeIds = dbNodes.map { it.id }.toSet()
        
        // Add new nodes from database
        dbNodes.forEach { dbNode ->
            if (!_nodeStates.containsKey(dbNode.id)) {
                _nodeStates[dbNode.id] = FileNodeUiState.fromFileNode(dbNode)
            }
            // Don't update existing nodes - they might be mid-drag
        }
        
        // Remove nodes that no longer exist in database
        val toRemove = _nodeStates.keys.filter { it !in dbNodeIds }
        toRemove.forEach { _nodeStates.remove(it) }
    }
    
    // ==================== DRAG OPERATIONS ====================
    
    /**
     * Update node position INSTANTLY.
     * Called DURING drag - updates the MutableState in FileNodeUiState.
     * This triggers immediate recomposition of the node AND connected lines.
     * NO DATABASE WRITE HERE!
     */
    fun updateNodePositionLive(nodeId: String, newX: Float, newY: Float) {
        _nodeStates[nodeId]?.updatePosition(newX, newY)
    }
    
    /**
     * Persist node position to database.
     * Called ONLY on drag END.
     */
    fun persistNodePosition(nodeId: String) {
        val uiState = _nodeStates[nodeId] ?: return
        viewModelScope.launch {
            fileRepository.updateNodePosition(nodeId, uiState.posX, uiState.posY)
        }
    }
    
    // ==================== MANUAL FILE ADDITION ====================
    
    fun addFileNode(
        uri: Uri,
        displayName: String,
        isFolder: Boolean,
        canvasCenterX: Float,
        canvasCenterY: Float
    ) {
        viewModelScope.launch {
            val nodeType = if (isFolder) FileNodeType.FOLDER else FileNodeType.FILE
            val nodeWidth = 100f
            val nodeHeight = 110f
            val posX = canvasCenterX - nodeWidth / 2f
            val posY = canvasCenterY - nodeHeight / 2f
            
            val newNode = FileNode(
                id = uri.toString(),
                displayName = displayName,
                posX = posX,
                posY = posY,
                parentPath = null,
                type = nodeType
            )
            
            fileRepository.insertNode(newNode)
        }
    }
    
    fun addMultipleFileNodes(
        files: List<Pair<Uri, String>>,
        canvasCenterX: Float,
        canvasCenterY: Float
    ) {
        viewModelScope.launch {
            val nodeWidth = 100f
            val nodeHeight = 110f
            val spacing = 20f
            
            files.forEachIndexed { index, (uri, displayName) ->
                val row = index / 4
                val col = index % 4
                val offsetX = (col - 1.5f) * (nodeWidth + spacing)
                val offsetY = row * (nodeHeight + spacing)
                
                val posX = canvasCenterX + offsetX - nodeWidth / 2f
                val posY = canvasCenterY + offsetY - nodeHeight / 2f
                
                val newNode = FileNode(
                    id = uri.toString(),
                    displayName = displayName,
                    posX = posX,
                    posY = posY,
                    parentPath = null,
                    type = FileNodeType.FILE
                )
                
                fileRepository.insertNode(newNode)
            }
        }
    }
    
    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            fileRepository.deleteNode(nodeId)
        }
    }
    
    // ==================== Selection ====================
    
    fun selectNode(nodeId: String) {
        _selectedNodeId.value = if (_selectedNodeId.value == nodeId) null else nodeId
    }
    
    fun clearSelection() {
        _selectedNodeId.value = null
    }
    
    // ==================== Connections ====================
    
    fun createConnection(sourcePath: String, targetPath: String) {
        viewModelScope.launch {
            fileRepository.createConnection(sourcePath, targetPath)
        }
    }
    
    fun deleteConnection(connectionId: Long) {
        viewModelScope.launch {
            fileRepository.deleteConnection(connectionId)
        }
    }
    
    /**
     * Delete connection between two specific nodes.
     */
    fun deleteConnectionBetween(path1: String, path2: String) {
        viewModelScope.launch {
            fileRepository.deleteConnectionBetween(path1, path2)
        }
    }
    
    /**
     * Get all node IDs connected to a specific node.
     */
    suspend fun getConnectedNodeIds(nodePath: String): List<String> {
        return fileRepository.getConnectedNodeIds(nodePath)
    }
    
    // ==================== Utility ====================
    
    fun clearError() {
        _error.value = null
    }
    
    fun getNodeById(nodeId: String): FileNodeUiState? {
        return _nodeStates[nodeId]
    }
}
