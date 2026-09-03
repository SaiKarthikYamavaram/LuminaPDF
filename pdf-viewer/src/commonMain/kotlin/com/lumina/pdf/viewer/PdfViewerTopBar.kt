package com.lumina.pdf.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumina.pdf.core.PdfRect
import com.lumina.pdf.viewer.theme.LuminaRadius
import kotlinx.coroutines.delay

private val BarHeight = 48.dp
private val ButtonSize = 36.dp
private val IconSize = 18.dp

@Composable
private fun TopBarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(22.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    )
}

@Composable
fun TopBar(
    title: String? = null,
    zoomPercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitPage: () -> Unit,
    nightMode: Boolean,
    onToggleNightMode: () -> Unit,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentMatchIndex: Int,
    totalMatches: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    isOutlineActive: Boolean,
    onOutlineActiveChange: (Boolean) -> Unit,
    isNotesActive: Boolean,
    onNotesActiveChange: (Boolean) -> Unit,
    currentPage: Int,
    pageCount: Int,
    onJumpToPage: (Int) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $pageCount):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                jumpPageInput = input
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetPage = jumpPageInput.toIntOrNull()
                        if (targetPage != null) {
                            val zeroIndexedPage = (targetPage - 1).coerceIn(0, pageCount - 1)
                            onJumpToPage(zeroIndexedPage)
                        }
                        showJumpDialog = false
                        jumpPageInput = ""
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(100)
            searchFocusRequester.requestFocus()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Status bar inset: translucent scrim (not a bar in its own right) so the system status
        // icons stay legible over whatever PDF content is behind it, while the actual toolbar
        // below is fully solid.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (isSearchActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SearchIcon(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(IconSize))

                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .focusRequester(searchFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                                    onSearchActiveChange(false)
                                    onSearchQueryChange("")
                                    true
                                } else false
                            }
                            .weight(1f)
                            .height(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(LuminaRadius.sm)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        val matchText = if (totalMatches > 0) {
                            "${currentMatchIndex + 1} / $totalMatches"
                        } else {
                            "0 / 0"
                        }
                        Text(
                            text = matchText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onPrevMatch,
                        enabled = totalMatches > 0,
                        modifier = Modifier.size(ButtonSize)
                    ) {
                        ChevronUpIcon(
                            color = if (totalMatches > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(IconSize)
                        )
                    }

                    IconButton(
                        onClick = onNextMatch,
                        enabled = totalMatches > 0,
                        modifier = Modifier.size(ButtonSize)
                    ) {
                        ChevronDownIcon(
                            color = if (totalMatches > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(IconSize)
                        )
                    }

                    IconButton(
                        onClick = {
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                        },
                        modifier = Modifier.size(ButtonSize)
                    ) {
                        CloseIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                    }
                }
            } else {
                var showOverflowMenu by remember { mutableStateOf(false) }

                // Left group: navigation / panel toggles. The back arrow is only shown
                // where there's no equivalent system-provided back navigation (Android
                // already has its own back gesture/button).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onClose != null && shouldShowInAppBackButton()) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(ButtonSize)
                        ) {
                            ArrowBackIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                        }
                    }

                    IconButton(
                        onClick = { onOutlineActiveChange(!isOutlineActive) },
                        modifier = Modifier
                            .size(ButtonSize)
                            .clip(RoundedCornerShape(LuminaRadius.sm))
                            .background(if (isOutlineActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        MenuIcon(
                            color = if (isOutlineActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize)
                        )
                    }

                    IconButton(
                        onClick = { onNotesActiveChange(!isNotesActive) },
                        modifier = Modifier
                            .size(ButtonSize)
                            .clip(RoundedCornerShape(LuminaRadius.sm))
                            .background(if (isNotesActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        NoteIcon(
                            color = if (isNotesActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(IconSize)
                        )
                    }
                }

                TopBarDivider()

                // Center: the open document's own name, not static app branding
                Text(
                    text = title?.takeIf { it.isNotBlank() } ?: "LuminaPDF",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )

                TopBarDivider()

                // Right group: kept to a small, fixed set of controls so it never needs
                // to compete with the title for space or overflow off-screen. Search and
                // the page indicator are core wayfinding, so they stay one tap away;
                // zoom/fit/night-mode are secondary and live behind the overflow menu.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onSearchActiveChange(true) },
                        modifier = Modifier.size(ButtonSize)
                    ) {
                        SearchIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                    }

                    // Page indicator, styled as a distinct pill so it reads as its own
                    // actionable control rather than blending into the icon buttons.
                    Surface(
                        onClick = {
                            jumpPageInput = (currentPage + 1).toString()
                            showJumpDialog = true
                        },
                        shape = RoundedCornerShape(LuminaRadius.pill),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.height(ButtonSize)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${currentPage + 1}/$pageCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(ButtonSize)
                        ) {
                            MoreVertIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Zoom in") },
                                leadingIcon = { AddIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize)) },
                                onClick = {
                                    onZoomIn()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Zoom out") },
                                leadingIcon = { RemoveIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize)) },
                                onClick = {
                                    onZoomOut()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit page ($zoomPercent%)") },
                                onClick = {
                                    onFitPage()
                                    showOverflowMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (nightMode) "Light mode" else "Night mode") },
                                leadingIcon = {
                                    if (nightMode) {
                                        SunIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                                    } else {
                                        MoonIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(IconSize))
                                    }
                                },
                                onClick = {
                                    onToggleNightMode()
                                    showOverflowMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
