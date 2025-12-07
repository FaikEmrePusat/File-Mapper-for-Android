package com.example.filemapper.ui.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.example.filemapper.data.local.entity.FileConnection
import com.example.filemapper.ui.state.ConnectionSelectionState
import com.example.filemapper.ui.state.DisconnectModeState
import com.example.filemapper.ui.state.FileNodeUiState

/**
 * Canvas content that renders nodes and connections.
 * Supports connection creation mode and disconnect mode.
 */
@Composable
fun CanvasContent(
    nodeStates: List<FileNodeUiState>,
    connections: List<FileConnection>,
    canvasState: CanvasState,
    connectionSelectionState: ConnectionSelectionState,
    disconnectModeState: DisconnectModeState,
    selectedNodeId: String? = null,
    onNodeDrag: (nodeId: String, newX: Float, newY: Float) -> Unit = { _, _, _ -> },
    onNodeDragEnd: (nodeId: String) -> Unit = {},
    onNodeClick: (FileNodeUiState) -> Unit = {},
    onStartConnectionMode: (nodeId: String) -> Unit = {},
    onStartDisconnectMode: (nodeId: String) -> Unit = {},
    onNodeDelete: (nodeId: String) -> Unit = {}
) {
    val nodeMap = nodeStates.associateBy { it.id }
    
    // Build set of nodes that have connections
    val nodesWithConnections = connections.flatMap { listOf(it.sourcePath, it.targetPath) }.toSet()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Connections
        ConnectionsLayer(
            connections = connections,
            nodeMap = nodeMap,
            disconnectModeState = disconnectModeState,
            modifier = Modifier.zIndex(0f)
        )
        
        // Layer 2: File nodes
        NodesLayer(
            nodeStates = nodeStates,
            canvasScale = canvasState.scale,
            selectedNodeId = selectedNodeId,
            connectionSelectionState = connectionSelectionState,
            disconnectModeState = disconnectModeState,
            nodesWithConnections = nodesWithConnections,
            onNodeDrag = onNodeDrag,
            onNodeDragEnd = onNodeDragEnd,
            onNodeClick = onNodeClick,
            onStartConnectionMode = onStartConnectionMode,
            onStartDisconnectMode = onStartDisconnectMode,
            onNodeDelete = onNodeDelete,
            modifier = Modifier.zIndex(1f)
        )
    }
}

@Composable
private fun ConnectionsLayer(
    connections: List<FileConnection>,
    nodeMap: Map<String, FileNodeUiState>,
    disconnectModeState: DisconnectModeState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        connections.forEach { connection ->
            val sourceNode = nodeMap[connection.sourcePath]
            val targetNode = nodeMap[connection.targetPath]
            
            // In disconnect mode, only show connections involving the source node
            if (disconnectModeState.isActive) {
                val sourceId = disconnectModeState.sourceNodeId
                val isRelevant = connection.sourcePath == sourceId || connection.targetPath == sourceId
                if (!isRelevant) return@forEach
            }
            
            if (sourceNode != null && targetNode != null) {
                val startPoint = calculateNodeCenter(sourceNode.posX, sourceNode.posY)
                val endPoint = calculateNodeCenter(targetNode.posX, targetNode.posY)
                
                GradientConnectionLine(
                    startPoint = startPoint,
                    endPoint = endPoint,
                    startColor = Color(0xFF6366F1),
                    endColor = Color(0xFF8B5CF6)
                )
            }
        }
    }
}

@Composable
private fun NodesLayer(
    nodeStates: List<FileNodeUiState>,
    canvasScale: Float,
    selectedNodeId: String?,
    connectionSelectionState: ConnectionSelectionState,
    disconnectModeState: DisconnectModeState,
    nodesWithConnections: Set<String>,
    onNodeDrag: (nodeId: String, newX: Float, newY: Float) -> Unit,
    onNodeDragEnd: (nodeId: String) -> Unit,
    onNodeClick: (FileNodeUiState) -> Unit,
    onStartConnectionMode: (nodeId: String) -> Unit,
    onStartDisconnectMode: (nodeId: String) -> Unit,
    onNodeDelete: (nodeId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        nodeStates.forEach { nodeState ->
            val isSelected = nodeState.id == selectedNodeId
            
            // Connection mode states
            val isConnectionSource = connectionSelectionState.sourceNodeId == nodeState.id
            val isConnectionTarget = connectionSelectionState.isTargetSelected(nodeState.id)
            val isInSelectionMode = connectionSelectionState.isActive
            
            // Disconnect mode states
            val isDisconnectSource = disconnectModeState.sourceNodeId == nodeState.id
            val isDisconnectTarget = disconnectModeState.isConnectedNeighbor(nodeState.id)
            val isSelectedForDisconnect = disconnectModeState.isSelectedForDisconnect(nodeState.id)
            val isDimmed = disconnectModeState.shouldDimNode(nodeState.id)
            
            // Check if node has any connections
            val hasConnections = nodesWithConnections.contains(nodeState.id)
            
            // Alpha for dimmed nodes in disconnect mode
            val alpha = if (isDimmed) 0.3f else 1f
            
            Box(modifier = Modifier.alpha(alpha)) {
                FileNodeItem(
                    nodeState = nodeState,
                    canvasScale = canvasScale,
                    isSelected = isSelected,
                    isConnectionSource = isConnectionSource,
                    isConnectionTarget = isConnectionTarget,
                    isInSelectionMode = isInSelectionMode,
                    isDisconnectSource = isDisconnectSource,
                    isDisconnectTarget = isDisconnectTarget,
                    isSelectedForDisconnect = isSelectedForDisconnect,
                    isDimmed = isDimmed,
                    hasConnections = hasConnections,
                    onDrag = onNodeDrag,
                    onDragEnd = onNodeDragEnd,
                    onClick = onNodeClick,
                    onStartConnectionMode = onStartConnectionMode,
                    onStartDisconnectMode = onStartDisconnectMode,
                    onDelete = onNodeDelete
                )
            }
        }
    }
}
