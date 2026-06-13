package com.lumina.pdf

import androidx.compose.animation.*
import androidx.compose.foundation.*
import com.lumina.pdf.viewer.db.rememberPdfDatabase
import com.lumina.pdf.viewer.*
import co.touchlab.kermit.Logger
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.*
import luminapdf.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RecentFile(
    val id: String,
    val name: String,
    val lastOpened: String,
    val size: String,
    val isSample: Boolean = false,
    val fileBytes: ByteArray? = null,
    val thumbnail: ImageBitmap? = null,
    val inRecents: Boolean = true
)



fun formatRelativeTime(timestamp: Long): String {
    val diff = com.lumina.pdf.viewer.db.getCurrentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    if (seconds < 60) return "Just now"
    val minutes = seconds / 60
    if (minutes < 60) return "$minutes minute${if (minutes > 1) "s" else ""} ago"
    val hours = minutes / 60
    if (hours < 24) return "$hours hour${if (hours > 1) "s" else ""} ago"
    val days = hours / 24
    return "$days day${if (days > 1) "s" else ""} ago"
}

// --- Home Screen UI ---

@Composable
fun HomeScreen(
    recentFiles: List<RecentFile>,
    onOpenFile: (RecentFile, Int?) -> Unit,
    onRemoveFromRecents: (RecentFile) -> Unit,
    onDeleteDocument: (RecentFile) -> Unit,
    onTriggerFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val db = rememberPdfDatabase()
    var activeTab by remember { mutableStateOf("Recents") }
    
    val notesGroupedByFile by produceState<Map<RecentFile, List<com.lumina.pdf.viewer.db.DbNote>>>(
        initialValue = emptyMap(),
        key1 = recentFiles.toList(),
        key2 = activeTab
    ) {
        if (activeTab == "Notes") {
            val map = mutableMapOf<RecentFile, List<com.lumina.pdf.viewer.db.DbNote>>()
            recentFiles.forEach { file ->
                try {
                    val fileNotes = db.getNotes(file.id)
                    if (fileNotes.isNotEmpty()) {
                        map[file] = fileNotes
                    }
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to get notes for file: ${file.id}" }
                }
            }
            value = map
        } else {
            value = emptyMap()
        }
    }
    var sortKey by remember { mutableStateOf("Recent") } // Recent, Name, Size
    var sortAscending by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var globalSearchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("Grid") } // Grid, List

    val darkBg = Color(0xFF1E1E1E)
    val panelBg = Color(0xFF171717)
    val textPrimary = Color(0xFFE5E5E5)
    val textSecondary = Color(0xFF999999)
    val blueAccent = Color(0xFF1473E6)
    val dividerColor = Color(0xFF2E2E2E)

    // Filter and Sort files
    val filteredFiles by remember(filterQuery, globalSearchQuery, sortKey, sortAscending, activeTab) {
        derivedStateOf {
            var list = recentFiles.filter {
                it.name.contains(filterQuery, ignoreCase = true) &&
                it.name.contains(globalSearchQuery, ignoreCase = true)
            }
            
            if (activeTab == "Recents") {
                list = list.filter { it.inRecents }
            }
            
            if (activeTab != "Recents") {
                list = when (sortKey) {
                    "Name" -> if (sortAscending) list.sortedBy { it.name.lowercase() } else list.sortedByDescending { it.name.lowercase() }
                    "Size" -> {
                        val sizeVal = { f: RecentFile ->
                            val s = f.size.uppercase()
                            val numeric = s.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 0.0
                            when {
                                s.contains("GB") -> numeric * 1024 * 1024
                                s.contains("MB") -> numeric * 1024
                                else -> numeric
                            }
                        }
                        if (sortAscending) list.sortedBy(sizeVal) else list.sortedByDescending(sizeVal)
                    }
                    else -> {
                        if (sortAscending) list.reversed() else list
                    }
                }
            }
            list
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        // 1. LEFT SIDEBAR PANEL
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(panelBg)
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Acrobat Styled Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                AcrobatPdfIcon()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LuminaPDF",
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                )
            }

            // Clean "Open File" button
            Button(
                onClick = onTriggerFilePicker,
                colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FolderIcon(color = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open File",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // Sidebar Navigation Tabs
            val navigationItems = listOf(
                Triple("Recents", @Composable { color: Color -> HomeIcon(color) }, "Recents"),
                Triple("Your documents", @Composable { color: Color -> FolderIcon(color) }, "Your documents"),
                Triple("Notes", @Composable { color: Color -> NotesIndicatorIcon(color) }, "Notes")
            )
            
            navigationItems.forEach { (tabName, icon, targetTab) ->
                val isActive = activeTab == targetTab
                HoverableItem(
                    onClick = { activeTab = targetTab },
                    normalColor = if (isActive) Color(0xFF2B2B2B) else Color.Transparent,
                    hoverColor = if (isActive) Color(0xFF2B2B2B) else Color(0xFF202020),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        icon(if (isActive) Color.White else textSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tabName,
                            color = if (isActive) Color.White else textSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. MAIN CONTENT AREA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // TOP BAR with modern search box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(panelBg)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(Color(0xFF252525), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp)
                ) {
                    SearchIcon(color = textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = globalSearchQuery,
                        onValueChange = { globalSearchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(blueAccent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (globalSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search documents...",
                                    color = Color(0xFF666666),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Workspace Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                Text(
                    text = activeTab,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (activeTab != "Notes") {
                    // Toolbar Row (Filter, Sort, View mode switcher)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        // Filter field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .width(240.dp)
                                .height(32.dp)
                                .background(Color(0xFF252525), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "Filter",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            BasicTextField(
                                value = filterQuery,
                                onValueChange = { filterQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = textPrimary, fontSize = 12.sp),
                                cursorBrush = SolidColor(blueAccent),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (filterQuery.isEmpty()) {
                                        Text(
                                            text = "Filter files...",
                                            color = Color(0xFF666666),
                                            fontSize = 12.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }

                        // Sort options & View switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (activeTab != "Recents") {
                                // Sort Dropdown
                                var showSortDropdown by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(
                                        onClick = { showSortDropdown = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = textPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Sort: $sortKey", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "▼", fontSize = 9.sp, color = textSecondary)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showSortDropdown,
                                        onDismissRequest = { showSortDropdown = false },
                                        modifier = Modifier.background(Color(0xFF252525))
                                    ) {
                                        val sortKeys = listOf("Recent", "Name", "Size")
                                        sortKeys.forEach { key ->
                                            DropdownMenuItem(
                                                text = { Text(text = key, color = textPrimary) },
                                                onClick = {
                                                    sortKey = key
                                                    showSortDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Sort direction toggle
                                IconButton(
                                    onClick = { sortAscending = !sortAscending },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    SortArrowIcon(isAscending = sortAscending, color = textPrimary)
                                }
                            }

                            // Segmented list/grid view switcher
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF252525), RoundedCornerShape(6.dp))
                                    .padding(2.dp)
                            ) {
                                val isList = viewMode == "List"
                                val isGrid = viewMode == "Grid"
                                
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(if (isList) Color(0xFF333333) else Color.Transparent, RoundedCornerShape(4.dp))
                                        .clickable { viewMode = "List" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ListIcon(color = if (isList) Color.White else textSecondary)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(if (isGrid) Color(0xFF333333) else Color.Transparent, RoundedCornerShape(4.dp))
                                        .clickable { viewMode = "Grid" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    GridIcon(color = if (isGrid) Color.White else textSecondary)
                                }
                            }
                        }
                    }
                }

                if (activeTab == "Notes") {
                    if (notesGroupedByFile.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                NotesIndicatorIcon(color = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = "No notes found in your documents", color = textSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            notesGroupedByFile.forEach { (file, notesList) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(panelBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, dividerColor, RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    // PDF Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp, 48.dp)
                                                .background(Color(0xFF151515), RoundedCornerShape(4.dp))
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (file.thumbnail != null) {
                                                Image(
                                                    bitmap = file.thumbnail,
                                                    contentDescription = file.name,
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier.fillMaxSize().padding(2.dp)
                                                )
                                            } else {
                                                AcrobatPdfIcon(modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = file.name,
                                                color = textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${notesList.size} annotation${if (notesList.size > 1) "s" else ""}",
                                                color = textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    // Notes list under this PDF
                                    notesList.forEachIndexed { index, note ->
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(end = 16.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(blueAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "Page ${note.page + 1}",
                                                            color = blueAccent,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = formatRelativeTime(note.timestamp),
                                                        color = textSecondary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                Text(
                                                    text = note.text,
                                                    color = textPrimary,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                            
                                            // Take to source button
                                            Button(
                                                onClick = { onOpenFile(file, note.page) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Take to source",
                                                        color = textPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    ArrowRightIcon(color = textPrimary, modifier = Modifier.size(10.dp))
                                                }
                                            }
                                        }
                                        
                                        if (index < notesList.size - 1) {
                                            HorizontalDivider(color = dividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Grid / List rendering of files
                    if (filteredFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No files found", color = textSecondary, fontSize = 14.sp)
                        }
                    } else {
                        if (viewMode == "Grid") {
                            val columns = 4
                            val rows = (filteredFiles.size + columns - 1) / columns
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (r in 0 until rows) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        for (c in 0 until columns) {
                                            val index = r * columns + c
                                            if (index < filteredFiles.size) {
                                                val fileItem = filteredFiles[index]
                                                Box(modifier = Modifier.weight(1f)) {
                                                    GridItemCard(
                                                        fileItem = fileItem,
                                                        db = db,
                                                        onClick = { onOpenFile(fileItem, null) },
                                                        onRemove = { if (activeTab == "Recents") onRemoveFromRecents(fileItem) else onDeleteDocument(fileItem) },
                                                        textPrimary = textPrimary,
                                                        textSecondary = textSecondary
                                                    )
                                                }
                                            } else {
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, dividerColor, RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1D1D1D))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .background(Color(0xFF222222))
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(text = "Name", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text(text = "Last Opened", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                                    Text(text = "Size", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                    Spacer(modifier = Modifier.width(32.dp))
                                }

                                filteredFiles.forEachIndexed { i, fileItem ->
                                    if (i > 0) {
                                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                    }
                                    ListItemRow(
                                        fileItem = fileItem,
                                        db = db,
                                        onClick = { onOpenFile(fileItem, null) },
                                        onRemove = { if (activeTab == "Recents") onRemoveFromRecents(fileItem) else onDeleteDocument(fileItem) },
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


