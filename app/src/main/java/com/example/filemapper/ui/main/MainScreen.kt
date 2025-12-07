package com.example.filemapper.ui.main

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.filemapper.ui.canvas.CanvasContent
import com.example.filemapper.ui.canvas.InfiniteCanvas
import com.example.filemapper.ui.canvas.rememberCanvasState
import com.example.filemapper.ui.state.ConnectionSelectionState
import com.example.filemapper.ui.state.DisconnectModeState
import com.example.filemapper.ui.theme.NeonBlue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Main screen with FAB dropdown for file/folder selection.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val selectedNodeId by viewModel.selectedNodeId.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val nodeStates = viewModel.nodeStates
    val connections = viewModel.connections
    
    val canvasState = rememberCanvasState()
    val connectionSelectionState = remember { ConnectionSelectionState() }
    val disconnectModeState = remember { DisconnectModeState() }
    
    // Coroutine scope for suspend functions
    val scope = rememberCoroutineScope()
    
    // FAB menu state
    var showAddMenu by remember { mutableStateOf(false) }
    
    // Helper to calculate canvas center
    fun getCanvasCenter() = canvasState.screenToCanvas(
        with(density) { configuration.screenWidthDp.dp.toPx() } / 2f,
        with(density) { configuration.screenHeightDp.dp.toPx() } / 2f
    )
    
    // File picker (multiple files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val canvasCenter = getCanvasCenter()
            
            // Take persistable permission for each file
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) { }
            }
            
            val filesWithNames = uris.mapNotNull { uri ->
                val displayName = getFileName(context, uri) ?: "Unknown"
                Pair(uri, displayName)
            }
            
            if (filesWithNames.size == 1) {
                val (uri, name) = filesWithNames.first()
                viewModel.addFileNode(uri, name, false, canvasCenter.x, canvasCenter.y)
                Toast.makeText(context, "Eklendi: $name", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addMultipleFileNodes(filesWithNames, canvasCenter.x, canvasCenter.y)
                Toast.makeText(context, "${filesWithNames.size} dosya eklendi", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Folder picker (OpenDocumentTree)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val canvasCenter = getCanvasCenter()
            
            // CRITICAL: Take persistable permission for folder access
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                Toast.makeText(context, "İzin alınamadı: ${e.message}", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            
            // Get folder name from URI
            val folderName = getFolderName(context, uri)
            
            viewModel.addFileNode(
                uri = uri,
                displayName = folderName,
                isFolder = true,
                canvasCenterX = canvasCenter.x,
                canvasCenterY = canvasCenter.y
            )
            
            Toast.makeText(context, "Klasör eklendi: $folderName", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Double tap tracking
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapNodeId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        floatingActionButton = {
            // Hide FAB during any selection mode
            if (!connectionSelectionState.isActive && !disconnectModeState.isActive) {
                Box {
                    FloatingActionButton(
                        onClick = { showAddMenu = true },
                        shape = CircleShape,
                        containerColor = NeonBlue,
                        contentColor = Color.Black,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add", Modifier.size(32.dp))
                    }
                    
                    // Dropdown menu for file/folder selection
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Dosya Ekle") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, null, tint = Color(0xFF6366F1))
                            },
                            onClick = {
                                showAddMenu = false
                                filePickerLauncher.launch(arrayOf("*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Klasör Ekle") },
                            leadingIcon = {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF4A90A4))
                            },
                            onClick = {
                                showAddMenu = false
                                folderPickerLauncher.launch(null)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            InfiniteCanvas(state = canvasState, modifier = Modifier.fillMaxSize()) {
                CanvasContent(
                    nodeStates = nodeStates.values.toList(),
                    connections = connections.values.toList(),
                    canvasState = canvasState,
                    connectionSelectionState = connectionSelectionState,
                    disconnectModeState = disconnectModeState,
                    selectedNodeId = selectedNodeId,
                    onNodeDrag = { nodeId, newX, newY ->
                        viewModel.updateNodePositionLive(nodeId, newX, newY)
                    },
                    onNodeDragEnd = { nodeId ->
                        viewModel.persistNodePosition(nodeId)
                    },
                    onNodeClick = { nodeState ->
                        when {
                            disconnectModeState.isActive -> {
                                // In disconnect mode - toggle selection for deletion
                                if (disconnectModeState.isConnectedNeighbor(nodeState.id)) {
                                    disconnectModeState.toggleSelection(nodeState.id)
                                }
                            }
                            connectionSelectionState.isActive -> {
                                connectionSelectionState.toggleTargetSelection(nodeState.id)
                            }
                            else -> {
                                val currentTime = System.currentTimeMillis()
                                if (lastTapNodeId == nodeState.id && currentTime - lastTapTime < 300) {
                                    openFileOrFolder(context, nodeState)
                                    lastTapNodeId = null
                                    lastTapTime = 0
                                } else {
                                    viewModel.selectNode(nodeState.id)
                                    lastTapNodeId = nodeState.id
                                    lastTapTime = currentTime
                                }
                            }
                        }
                    },
                    onStartConnectionMode = { sourceNodeId ->
                        connectionSelectionState.startSelectionMode(sourceNodeId)
                        Toast.makeText(context, "Hedef node'ları seçin", Toast.LENGTH_SHORT).show()
                    },
                    onStartDisconnectMode = { sourceNodeId ->
                        scope.launch {
                            val connectedIds = viewModel.getConnectedNodeIds(sourceNodeId)
                            if (connectedIds.isNotEmpty()) {
                                disconnectModeState.startDisconnectMode(sourceNodeId, connectedIds)
                                Toast.makeText(context, "Kesmek için bağlı node'a dokunun", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Bu node'un bağlantısı yok", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onNodeDelete = { nodeId ->
                        viewModel.deleteNode(nodeId)
                        Toast.makeText(context, "Haritadan kaldırıldı", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            // Empty state
            if (nodeStates.isEmpty() && !connectionSelectionState.isActive) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dosya veya klasör eklemek için", color = Color.White.copy(alpha = 0.5f))
                        Text("+ butonuna dokunun", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
            
            // Selection mode bar
            AnimatedVisibility(
                visible = connectionSelectionState.isActive,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SelectionModeBar(
                    selectedCount = connectionSelectionState.selectedCount,
                    onConnect = {
                        val pairs = connectionSelectionState.complete()
                        pairs.forEach { (source, target) ->
                            viewModel.createConnection(source, target)
                        }
                        if (pairs.isNotEmpty()) {
                            Toast.makeText(context, "${pairs.size} bağlantı oluşturuldu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = { connectionSelectionState.cancel() }
                )
            }
            
            // Disconnect mode bar
            AnimatedVisibility(
                visible = disconnectModeState.isActive,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                DisconnectModeBar(
                    selectedCount = disconnectModeState.selectedCount,
                    onDisconnect = {
                        val pairs = disconnectModeState.complete()
                        pairs.forEach { (source, target) ->
                            viewModel.deleteConnectionBetween(source, target)
                        }
                        if (pairs.isNotEmpty()) {
                            Toast.makeText(context, "${pairs.size} bağlantı kesildi", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = { disconnectModeState.cancel() }
                )
            }
            
            // Error snackbar
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Kapat", color = Color.White)
                        }
                    }
                ) { Text(errorMessage) }
            }
        }
    }
}

@Composable
private fun SelectionModeBar(
    selectedCount: Int,
    onConnect: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E2E), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("İptal")
        }
        
        Text(
            text = if (selectedCount == 0) "Hedef seçin" else "$selectedCount hedef seçildi",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        
        Button(
            onClick = onConnect,
            enabled = selectedCount > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Bağla")
        }
    }
}

/**
 * Bottom bar for disconnect mode.
 */
@Composable
private fun DisconnectModeBar(
    selectedCount: Int,
    onDisconnect: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D1F1F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel button
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("İptal")
        }
        
        // Status text
        Text(
            text = if (selectedCount == 0) "Kesilecek bağlantı seçin" else "$selectedCount bağlantı seçildi",
            color = Color(0xFFFF9800),
            fontSize = 14.sp
        )
        
        // Disconnect button
        Button(
            onClick = onDisconnect,
            enabled = selectedCount > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Kes")
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) result = cursor.getString(nameIndex)
            }
        }
    }
    if (result == null) result = uri.path?.substringAfterLast('/')
    return result
}

private fun getFolderName(context: android.content.Context, uri: Uri): String {
    // Try to get folder name from DocumentsContract
    try {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        context.contentResolver.query(docUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
    } catch (_: Exception) { }
    
    // Fallback: extract from path
    return uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/') ?: "Klasör"
}

/**
 * Open a file or folder using system Intent.
 */
private fun openFileOrFolder(
    context: android.content.Context,
    nodeState: com.example.filemapper.ui.state.FileNodeUiState
) {
    try {
        val uri = Uri.parse(nodeState.id)
        
        if (nodeState.type == com.example.filemapper.data.local.entity.FileNodeType.FOLDER) {
            // Open folder in document browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Try to open with Documents UI
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback: try to browse the folder tree
                val browseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
                context.startActivity(browseIntent)
            }
        } else {
            // Open file with appropriate app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(context, uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Bu dosyayı açacak uygulama bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Açılırken hata: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Get MIME type for a file URI.
 */
private fun getMimeType(context: android.content.Context, uri: Uri): String {
    return context.contentResolver.getType(uri) ?: "*/*"
}

