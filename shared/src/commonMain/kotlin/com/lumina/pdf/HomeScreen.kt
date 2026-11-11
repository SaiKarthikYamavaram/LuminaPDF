package com.lumina.pdf

import androidx.compose.animation.*
import androidx.compose.foundation.*
import com.lumina.pdf.viewer.db.rememberPdfDatabase
import com.lumina.pdf.viewer.*
import co.touchlab.kermit.Logger
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.*
import luminapdf.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import com.lumina.pdf.viewer.theme.LuminaRadius
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class RecentFile(
    val id: String,
    val name: String,
    // Epoch millis; 0 = unknown (never opened, or a row saved before this was a timestamp).
    val lastOpened: Long,
    val size: String,
    val isSample: Boolean = false,
    // Size of the stored PDF. The bytes stay on disk until the document is opened.
    val byteCount: Int = 0,
    val thumbnail: ImageBitmap? = null,
    val inRecents: Boolean = true,
    // Single optional folder name; "" = unfiled. One level only - no nesting.
    val folder: String = ""
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

fun RecentFile.lastOpenedLabel(): String = if (lastOpened > 0) formatRelativeTime(lastOpened) else ""

// --- Home Screen UI ---

@Composable
fun HomeScreen(
    recentFiles: List<RecentFile>,
    sampleFile: RecentFile? = null,
    onOpenFile: (RecentFile, Int?) -> Unit,
    onRemoveFromRecents: (RecentFile) -> Unit,
    onDeleteDocument: (RecentFile) -> Unit,
    onEditDocument: (file: RecentFile, name: String, folder: String) -> Unit,
    onTriggerFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val db = rememberPdfDatabase()
    var renamingFile by remember { mutableStateOf<RecentFile?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var folderInput by remember { mutableStateOf("") }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    val folders = recentFiles.map { it.folder }.filter { it.isNotEmpty() }.distinct().sorted()
    // Falls back to "All" once the last document leaves the selected folder, instead of
    // stranding the user on an empty, chip-less filter.
    val activeFolder = selectedFolder?.takeIf { it in folders }
    val startEditing: (RecentFile) -> Unit = { file ->
        renamingFile = file
        renameInput = file.name
        folderInput = file.folder
    }

    renamingFile?.let { file ->
        AlertDialog(
            onDismissRequest = { renamingFile = null },
            title = { Text("Edit document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { folderInput = it },
                        label = { Text("Folder (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (folders.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            folders.forEach { name ->
                                SuggestionChip(onClick = { folderInput = name }, label = { Text(name) })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty()) onEditDocument(file, newName, folderInput.trim())
                        renamingFile = null
                    },
                    enabled = renameInput.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingFile = null }) { Text("Cancel") }
            }
        )
    }
    var activeTab by rememberSaveable { mutableStateOf("Recents") }
    
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
    // Batched once for every card instead of each GridItemCard/ListItemRow querying the DB itself
    // (that was N queries fired at once on every home-screen composition for N recent files).
    val documentIdsWithNotes by produceState(initialValue = emptySet<String>(), key1 = recentFiles) {
        value = db.getDocumentIdsWithNotes()
    }
    var sortKey by rememberSaveable { mutableStateOf("Recent") } // Recent, Name, Size
    var sortAscending by rememberSaveable { mutableStateOf(false) }
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    var viewMode by rememberSaveable { mutableStateOf("Grid") } // Grid, List

    // Narrows the Notes tab by the same search bar as Recents/Your documents - matches either the
    // owning document's name or a note's own text, so searching a document name surfaces all its
    // notes while searching a phrase surfaces just the notes containing it.
    val filteredNotesGroupedByFile by remember(notesGroupedByFile, globalSearchQuery) {
        derivedStateOf {
            if (globalSearchQuery.isBlank()) {
                notesGroupedByFile
            } else {
                notesGroupedByFile.mapNotNull { (file, notes) ->
                    val matchingNotes = if (file.name.contains(globalSearchQuery, ignoreCase = true)) {
                        notes
                    } else {
                        notes.filter { it.text.contains(globalSearchQuery, ignoreCase = true) }
                    }
                    if (matchingNotes.isNotEmpty()) file to matchingNotes else null
                }.toMap()
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val darkBg = colorScheme.background
    val panelBg = colorScheme.surfaceContainerLow
    val textPrimary = colorScheme.onSurface
    val textSecondary = colorScheme.onSurfaceVariant
    val blueAccent = colorScheme.primary
    val dividerColor = colorScheme.outlineVariant

    // Filter and Sort files
    val filteredFiles by remember(globalSearchQuery, sortKey, sortAscending, activeTab, activeFolder) {
        derivedStateOf {
            var list = recentFiles.filter {
                it.name.contains(globalSearchQuery, ignoreCase = true)
            }
            
            if (activeTab == "Recents") {
                list = list.filter { it.inRecents }
            }
            if (activeTab == "Your documents" && activeFolder != null) {
                list = list.filter { it.folder == activeFolder }
            }
            
            if (activeTab != "Recents") {
                list = when (sortKey) {
                    "Name" -> if (sortAscending) list.sortedBy { it.name.lowercase() } else list.sortedByDescending { it.name.lowercase() }
                    "Size" -> if (sortAscending) list.sortedBy { it.byteCount } else list.sortedByDescending { it.byteCount }
                    else -> {
                        if (sortAscending) list.reversed() else list
                    }
                }
            }
            list
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
            .safeDrawingPadding()
    ) {
        // Below this width the fixed 240dp sidebar left almost no room for content,
        // so phones get a single-pane layout with a compact header + scrollable tab row instead.
        val isCompact = maxWidth < 700.dp
        val gridColumns = if (isCompact) {
            (maxWidth / 170.dp).toInt().coerceIn(1, 3)
        } else {
            ((maxWidth - 240.dp) / 170.dp).toInt().coerceIn(2, 6)
        }

        // Nav tabs, shared between the vertical sidebar (wide) and horizontal tab row (compact).
        // A plain dot per the Figma revamp (nav-item glyphs replaced with a status-style bullet)
        // rather than a distinct icon per tab.
        val navDot: @Composable (Color) -> Unit = { color ->
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        }
        val navigationItems = listOf(
            Triple("Recents", navDot, "Recents"),
            Triple("Your documents", navDot, "Your documents"),
            Triple("Notes", navDot, "Notes")
        )

        val logo: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogoMark()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "LuminaPDF",
                    style = TextStyle(
                        color = textPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        val versionFooter: @Composable () -> Unit = {
            val versionLabel = remember { appVersionLabel() }
            if (versionLabel.isNotEmpty()) {
                Text(
                    text = versionLabel,
                    style = TextStyle(color = textSecondary, fontSize = 10.sp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
            }
        }

        val openFileButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            Button(
                onClick = onTriggerFilePicker,
                colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                shape = RoundedCornerShape(LuminaRadius.md),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = buttonModifier
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FolderIcon(color = colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open File",
                        color = colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Search bar + scrollable workspace (files grid/list or notes) — shared between
        // the wide two-pane layout and the compact single-pane layout.
        val mainContent: @Composable (Modifier) -> Unit = { contentModifier ->
            Column(modifier = contentModifier) {
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
                        .background(colorScheme.surfaceContainerHigh, RoundedCornerShape(LuminaRadius.md))
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(LuminaRadius.md))
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
                                    color = textSecondary.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Section title (Recents / Your documents / Notes) - the Figma revamp restores a
            // page heading here; it was previously dropped as redundant with the tab bar above,
            // but the design now expects it as the first thing under the search bar.
            Text(
                text = activeTab,
                color = textPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp)
            )

            if (activeTab == "Your documents") {
                // Storage usage: every stored document is a full copy in app storage, so this
                // is the real on-disk footprint. Freeing space = Sort: Size, then remove.
                val storedFiles = recentFiles.filter { !it.isSample }
                Text(
                    text = "${storedFiles.size} document${if (storedFiles.size == 1) "" else "s"} · " +
                        "${formatSize(storedFiles.sumOf { it.byteCount })} stored",
                    color = textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 4.dp)
                )
                if (folders.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 28.dp, end = 28.dp, top = 12.dp)
                    ) {
                        FilterChip(
                            selected = activeFolder == null,
                            onClick = { selectedFolder = null },
                            label = { Text("All") }
                        )
                        folders.forEach { name ->
                            FilterChip(
                                selected = activeFolder == name,
                                onClick = { selectedFolder = name },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            // Workspace Content. Header + toolbar are rendered once, as a fixed sibling above
            // the scrollable body below - not inside either scrollable container itself. Grid
            // and List view swap between fundamentally different container types (LazyVerticalGrid
            // vs. a plain scrolling Column), so a header composed as part of *either one* gets
            // torn down and remounted every time viewMode toggles, which visibly jumped/flickered
            // the Sort/List/Grid buttons on every switch.
            val workspaceHeader: @Composable () -> Unit = {
                // No repeated "Recents"/"Your documents"/"Notes" title here - the tab bar above
                // already shows which section is active, so restating it as a heading was
                // redundant. Sort/List/Grid stay as a toolbar row for the list below.
                //
                // The row itself always renders (even empty, on Notes) with a fixed minimum
                // height matching its tallest control (the switcher pill) - otherwise Notes,
                // which shows no controls at all, collapsed to zero height here and its content
                // started right under the search bar while every other tab left a real gap.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // Spread the two groups across the full row now that there's no title
                    // competing for the left side - Sort reads more naturally near the content
                    // it affects (top-left of the list) than crowded against the view switcher.
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 30.dp)
                        .padding(bottom = 20.dp)
                ) {
                    // Left: Sort controls - only "Your documents" supports sorting. Always wrapped
                    // in a Row (even empty) so this is a stable 2-child SpaceBetween regardless of
                    // tab; a single real child would collapse to the start instead of staying put.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeTab != "Notes" && activeTab != "Recents") {
                            // Sort Dropdown. A plain clickable Row instead of TextButton, and
                            // a fixed-size Box instead of IconButton below - Material's default
                            // min touch-target heights on those (~40dp/48dp) made this row
                            // taller than the plain "Recents" row (title + switcher only,
                            // ~28dp), which shifted the vertically-centered title down.
                            var showSortDropdown by remember { mutableStateOf(false) }
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(LuminaRadius.sm))
                                        // Merges the "Sort: $sortKey" text below into this
                                        // control's own accessible node instead of TalkBack
                                        // treating them as separate, disconnected stops.
                                        .semantics(mergeDescendants = true) {}
                                        .clickable { showSortDropdown = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "Sort: $sortKey", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "▼", fontSize = 9.sp, color = textSecondary)
                                }

                                DropdownMenu(
                                    expanded = showSortDropdown,
                                    onDismissRequest = { showSortDropdown = false },
                                    modifier = Modifier.background(colorScheme.surfaceContainerHigh)
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(LuminaRadius.sm))
                                    .clickable(
                                        onClickLabel = if (sortAscending) "Sort descending" else "Sort ascending"
                                    ) { sortAscending = !sortAscending }
                            ) {
                                SortArrowIcon(isAscending = sortAscending, color = textPrimary)
                            }
                        }
                    }

                    // Right: List/Grid view switcher - same always-a-Row treatment as the Sort
                    // slot above, so this row has exactly 2 children on every tab.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (activeTab != "Notes") {
                            Row(
                                modifier = Modifier
                                    .background(colorScheme.surfaceContainerHigh, RoundedCornerShape(LuminaRadius.sm))
                                    .padding(2.dp)
                            ) {
                                val isList = viewMode == "List"
                                val isGrid = viewMode == "Grid"

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(if (isList) colorScheme.outlineVariant else Color.Transparent, RoundedCornerShape(4.dp))
                                        .semantics { role = Role.RadioButton; selected = isList }
                                        .clickable(onClickLabel = "List view") { viewMode = "List" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ListIcon(color = if (isList) textPrimary else textSecondary)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(if (isGrid) colorScheme.outlineVariant else Color.Transparent, RoundedCornerShape(4.dp))
                                        .semantics { role = Role.RadioButton; selected = isGrid }
                                        .clickable(onClickLabel = "Grid view") { viewMode = "Grid" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    GridIcon(color = if (isGrid) textPrimary else textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 12.dp)
            ) {
                workspaceHeader()
            }

            if (activeTab != "Notes" && viewMode == "Grid" && filteredFiles.isNotEmpty()) {
                // Lazy grid: only the cards actually on screen compose, measure, and query
                // notes state — the old plain Column/Row grid built every recent-file card
                // up front regardless of scroll position.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { fileItem ->
                        GridItemCard(
                            fileItem = fileItem,
                            hasNotes = fileItem.id in documentIdsWithNotes,
                            onClick = { onOpenFile(fileItem, null) },
                            onRemove = { if (activeTab == "Recents") onRemoveFromRecents(fileItem) else onDeleteDocument(fileItem) },
                            onRename = { startEditing(fileItem) },
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 28.dp, end = 28.dp, bottom = 28.dp)
            ) {
                if (activeTab == "Notes") {
                    if (filteredNotesGroupedByFile.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                NotesIndicatorIcon(color = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (globalSearchQuery.isBlank()) {
                                        "No notes found in your documents"
                                    } else {
                                        "No notes match \"$globalSearchQuery\""
                                    },
                                    color = textSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredNotesGroupedByFile.forEach { (file, notesList) ->
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
                                                .background(colorScheme.surfaceContainerLowest, RoundedCornerShape(4.dp))
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
                                                AppLogoMark(modifier = Modifier.size(20.dp))
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
                                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.surfaceContainerHigh),
                                                shape = RoundedCornerShape(LuminaRadius.sm),
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
                        val isTrulyEmpty = recentFiles.isEmpty() && globalSearchQuery.isEmpty()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTrulyEmpty) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FolderIcon(color = textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = "No documents yet", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Open a PDF to get started", color = textSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = onTriggerFilePicker,
                                            colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                                            shape = RoundedCornerShape(LuminaRadius.md)
                                        ) {
                                            Text(text = "Open File", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        if (sampleFile != null) {
                                            OutlinedButton(
                                                onClick = { onOpenFile(sampleFile, null) },
                                                shape = RoundedCornerShape(LuminaRadius.md)
                                            ) {
                                                Text(text = "Try a sample PDF", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(text = "No files found", color = textSecondary, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Grid+non-empty is handled above by the lazy grid path; only List
                        // view reaches here.
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                // clip first: background(color) below has no shape (fills a plain
                                // rectangle) and the header row inside has its own square
                                // background, so without clipping both poked past the rounded
                                // border's corners instead of following it.
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, dividerColor, RoundedCornerShape(4.dp))
                                .background(colorScheme.surfaceContainerLow)
                        ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .background(colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(text = "Name", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    // On phone-width screens, Last Opened + Size + the remove-button
                                    // slot (120+80+32dp, plus this row's own 32dp padding) can eat
                                    // most of the available width, squeezing Name down to a few
                                    // characters - confirmed on a 560dpi/~354dp-wide device where it
                                    // left ~90dp for icon+spacer+name+notes combined. Dropping the
                                    // least essential column (Last Opened) below 700dp (same
                                    // threshold the rest of this screen uses for compact layout)
                                    // gives Name its width back.
                                    if (!isCompact) {
                                        Text(text = "Last Opened", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(120.dp))
                                    }
                                    Text(text = "Size", color = textSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                                    Spacer(modifier = Modifier.width(32.dp))
                                }

                                filteredFiles.forEachIndexed { i, fileItem ->
                                    if (i > 0) {
                                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                    }
                                    ListItemRow(
                                        fileItem = fileItem,
                                        hasNotes = fileItem.id in documentIdsWithNotes,
                                        showLastOpened = !isCompact,
                                        onClick = { onOpenFile(fileItem, null) },
                                        onRemove = { if (activeTab == "Recents") onRemoveFromRecents(fileItem) else onDeleteDocument(fileItem) },
                                        onRename = { startEditing(fileItem) },
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

        if (isCompact) {
            // Single-pane phone layout: compact header + a horizontally scrollable tab
            // row stand in for the permanent 240dp sidebar used on wider screens.
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    logo()
                    Spacer(modifier = Modifier.weight(1f))
                    openFileButton(Modifier)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelBg)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    navigationItems.forEach { (tabName, icon, targetTab) ->
                        val isActive = activeTab == targetTab
                        val activeColor = colorScheme.primary
                        HoverableItem(
                            onClick = { activeTab = targetTab },
                            normalColor = if (isActive) colorScheme.primaryContainer else Color.Transparent,
                            hoverColor = if (isActive) colorScheme.primaryContainer else colorScheme.onSurface.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(LuminaRadius.md),
                            modifier = Modifier
                                .height(36.dp)
                                .semantics { role = Role.Tab; selected = isActive }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp)
                            ) {
                                icon(if (isActive) activeColor else textSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tabName,
                                    color = if (isActive) activeColor else textSecondary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = dividerColor)

                mainContent(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    versionFooter()
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. LEFT SIDEBAR PANEL
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(panelBg)
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    // Acrobat Styled Logo
                    Box(modifier = Modifier.padding(bottom = 24.dp)) {
                        logo()
                    }

                    // Clean "Open File" button
                    openFileButton(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    navigationItems.forEach { (tabName, icon, targetTab) ->
                        val isActive = activeTab == targetTab
                        val activeColor = colorScheme.primary
                        HoverableItem(
                            onClick = { activeTab = targetTab },
                            normalColor = if (isActive) colorScheme.primaryContainer else Color.Transparent,
                            hoverColor = if (isActive) colorScheme.primaryContainer else colorScheme.onSurface.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(LuminaRadius.md),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(vertical = 2.dp)
                                .semantics { role = Role.Tab; selected = isActive }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                icon(if (isActive) activeColor else textSecondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = tabName,
                                    color = if (isActive) activeColor else textSecondary,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    versionFooter()
                }

                // 2. MAIN CONTENT AREA
                mainContent(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}


