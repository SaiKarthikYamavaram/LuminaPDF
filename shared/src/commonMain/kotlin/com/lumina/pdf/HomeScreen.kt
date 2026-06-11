package com.lumina.pdf

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
    val thumbnail: ImageBitmap? = null
)

// --- Custom Canvas Icons for Sleek, Dependency-free Rendering ---

@Composable
fun AcrobatPdfIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(32.dp)) {
        drawRoundRect(
            color = Color(0xFF2E2E2E), // Dark rounded rectangle background
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        
        // Acrobat infinity-ribbon logo drawn via triangular tri-loop path
        val path = Path().apply {
            val cx = w * 0.5f
            val cy = h * 0.5f
            val sizeVal = w * 0.28f
            
            // Top loop
            moveTo(cx, cy - 2.dp.toPx())
            cubicTo(cx - sizeVal * 0.6f, cy - sizeVal * 1.5f, cx + sizeVal * 0.6f, cy - sizeVal * 1.5f, cx, cy - 2.dp.toPx())
            
            // Bottom-left loop
            moveTo(cx - 2.dp.toPx(), cy + 1.dp.toPx())
            cubicTo(cx - sizeVal * 1.4f, cy + sizeVal * 0.2f, cx - sizeVal * 0.8f, cy + sizeVal * 1.3f, cx - 2.dp.toPx(), cy + 1.dp.toPx())
            
            // Bottom-right loop
            moveTo(cx + 2.dp.toPx(), cy + 1.dp.toPx())
            cubicTo(cx + sizeVal * 1.4f, cy + sizeVal * 0.2f, cx + sizeVal * 0.8f, cy + sizeVal * 1.3f, cx + 2.dp.toPx(), cy + 1.dp.toPx())
            
            // Connecting center crossovers
            moveTo(cx, cy - 2.dp.toPx())
            lineTo(cx - 2.dp.toPx(), cy + 1.dp.toPx())
            lineTo(cx + 2.dp.toPx(), cy + 1.dp.toPx())
            close()
        }
        drawPath(path = path, color = Color.White, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HomeIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.45f)
            lineTo(w * 0.85f, h * 0.85f)
            close()
            // Door
            moveTo(w * 0.4f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.55f)
            lineTo(w * 0.6f, h * 0.55f)
            lineTo(w * 0.6f, h * 0.85f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
fun LearnIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        // Lightbulb
        drawCircle(
            color = color,
            radius = w * 0.22f,
            center = Offset(w * 0.5f, h * 0.4f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        val path = Path().apply {
            moveTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.38f, h * 0.78f)
            lineTo(w * 0.62f, h * 0.78f)
            lineTo(w * 0.62f, h * 0.62f)
            
            moveTo(w * 0.44f, h * 0.84f)
            lineTo(w * 0.56f, h * 0.84f)
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx()))
    }
}

@Composable
fun FolderIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.22f)
            lineTo(w * 0.4f, h * 0.22f)
            lineTo(w * 0.52f, h * 0.35f)
            lineTo(w * 0.9f, h * 0.35f)
            lineTo(w * 0.9f, h * 0.82f)
            lineTo(w * 0.1f, h * 0.82f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
fun PeopleIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        // Left person head
        drawCircle(
            color = color,
            radius = w * 0.11f,
            center = Offset(w * 0.38f, h * 0.38f),
            style = Stroke(width = 1.6.dp.toPx())
        )
        // Left person shoulders
        val pathL = Path().apply {
            moveTo(w * 0.15f, h * 0.75f)
            quadraticTo(w * 0.18f, h * 0.58f, w * 0.38f, h * 0.58f)
            quadraticTo(w * 0.58f, h * 0.58f, w * 0.61f, h * 0.75f)
        }
        drawPath(path = pathL, color = color, style = Stroke(width = 1.6.dp.toPx()))
        
        // Right person head (smaller, background)
        drawCircle(
            color = color,
            radius = w * 0.09f,
            center = Offset(w * 0.65f, h * 0.45f),
            style = Stroke(width = 1.6.dp.toPx())
        )
        val pathR = Path().apply {
            moveTo(w * 0.48f, h * 0.75f)
            quadraticTo(w * 0.52f, h * 0.64f, w * 0.65f, h * 0.64f)
            quadraticTo(w * 0.78f, h * 0.64f, w * 0.82f, h * 0.75f)
        }
        drawPath(path = pathR, color = color, style = Stroke(width = 1.6.dp.toPx()))
    }
}

@Composable
fun CloudIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.7f)
            quadraticTo(w * 0.12f, h * 0.7f, w * 0.12f, h * 0.55f)
            quadraticTo(w * 0.12f, h * 0.42f, w * 0.28f, h * 0.42f)
            quadraticTo(w * 0.35f, h * 0.25f, w * 0.52f, h * 0.25f)
            quadraticTo(w * 0.72f, h * 0.25f, w * 0.72f, h * 0.42f)
            quadraticTo(w * 0.88f, h * 0.42f, w * 0.88f, h * 0.58f)
            quadraticTo(w * 0.88f, h * 0.7f, w * 0.75f, h * 0.7f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
fun TrashIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // Bin outline
        val pathBin = Path().apply {
            moveTo(w * 0.22f, h * 0.32f)
            lineTo(w * 0.26f, h * 0.85f)
            lineTo(w * 0.74f, h * 0.85f)
            lineTo(w * 0.78f, h * 0.32f)
        }
        drawPath(path = pathBin, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
        // Lid
        drawLine(color = color, start = Offset(w * 0.12f, h * 0.32f), end = Offset(w * 0.88f, h * 0.32f), strokeWidth = stroke)
        // Handle
        val pathHandle = Path().apply {
            moveTo(w * 0.38f, h * 0.32f)
            lineTo(w * 0.38f, h * 0.22f)
            lineTo(w * 0.62f, h * 0.22f)
            lineTo(w * 0.62f, h * 0.32f)
        }
        drawPath(path = pathHandle, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
    }
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 4.5f
        val cx = size.width * 0.42f
        val cy = size.height * 0.42f
        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )
        val startX = (cx + r * cos(PI/4)).toFloat()
        val startY = (cy + r * sin(PI/4)).toFloat()
        val endX = size.width * 0.78f
        val endY = size.height * 0.78f
        drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = stroke)
    }
}

@Composable
fun GiftIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // Gift Box
        drawRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.4f),
            size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.45f),
            style = Stroke(width = stroke)
        )
        // Lid
        drawRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.3f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.12f),
            style = Stroke(width = stroke)
        )
        // Ribbon Vertical
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = stroke)
        // Bow (Left & Right loops)
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = Offset(w * 0.4f, h * 0.22f),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = Offset(w * 0.6f, h * 0.22f),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun GridIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        val sizeVal = w * 0.35f
        
        drawRect(color = color, topLeft = Offset(w * 0.1f, h * 0.1f), size = androidx.compose.ui.geometry.Size(sizeVal, sizeVal), style = Stroke(width = stroke))
        drawRect(color = color, topLeft = Offset(w * 0.55f, h * 0.1f), size = androidx.compose.ui.geometry.Size(sizeVal, sizeVal), style = Stroke(width = stroke))
        drawRect(color = color, topLeft = Offset(w * 0.1f, h * 0.55f), size = androidx.compose.ui.geometry.Size(sizeVal, sizeVal), style = Stroke(width = stroke))
        drawRect(color = color, topLeft = Offset(w * 0.55f, h * 0.55f), size = androidx.compose.ui.geometry.Size(sizeVal, sizeVal), style = Stroke(width = stroke))
    }
}

@Composable
fun ListIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        
        // Rows
        for (i in 0 until 3) {
            val y = h * (0.2f + i * 0.3f)
            // Bullet point
            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(w * 0.18f, y))
            // Line
            drawLine(color = color, start = Offset(w * 0.35f, y), end = Offset(w * 0.85f, y), strokeWidth = stroke)
        }
    }
}

@Composable
fun SortArrowIcon(isAscending: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        
        // Arrow shaft
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = stroke)
        
        if (isAscending) {
            // Pointing up
            drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.3f, h * 0.4f), strokeWidth = stroke)
            drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.7f, h * 0.4f), strokeWidth = stroke)
        } else {
            // Pointing down
            drawLine(color = color, start = Offset(w * 0.5f, h * 0.85f), end = Offset(w * 0.3f, h * 0.6f), strokeWidth = stroke)
            drawLine(color = color, start = Offset(w * 0.5f, h * 0.85f), end = Offset(w * 0.7f, h * 0.6f), strokeWidth = stroke)
        }
    }
}

@Composable
fun InfoIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = 1.6.dp.toPx()
        drawCircle(color = color, radius = size.minDimension / 2.2f, center = center, style = Stroke(width = stroke))
        // Dot
        drawCircle(color = color, radius = 1.2f.dp.toPx(), center = Offset(size.width / 2f, size.height * 0.32f))
        // Body of 'i'
        drawLine(color = color, start = Offset(size.width / 2f, size.height * 0.48f), end = Offset(size.width / 2f, size.height * 0.75f), strokeWidth = stroke + 0.5f)
    }
}

@Composable
fun SyncCheckIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        // Draw green circle
        drawCircle(color = color)
        // Draw white checkmark
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.5f)
            lineTo(size.width * 0.45f, size.height * 0.65f)
            lineTo(size.width * 0.75f, size.height * 0.35f)
        }
        drawPath(path = path, color = Color.White, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// --- Hoverable Item Wrapper ---
@Composable
fun HoverableItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    normalColor: Color = Color.Transparent,
    hoverColor: Color = Color(0xFF252525),
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    content: @Composable BoxScope.(Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    
    Box(
        modifier = modifier
            .background(if (isHovered) hoverColor else normalColor, shape)
            .clickable(onClick = onClick)
            .hoverable(interactionSource),
        content = { content(isHovered) }
    )
}

// --- Canvas Mock Document Page Preview ---
@Composable
fun MockDocumentThumbnail(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF151515))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // White page container
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f / 1.414f) // standard A4 aspect ratio
                .background(Color.White, RoundedCornerShape(1.dp))
                .border(1.5.dp, Color(0xFFDCDCDC), RoundedCornerShape(1.dp))
                .padding(8.dp)
        ) {
            when {
                name.contains("Agreement", ignoreCase = true) -> {
                    // Vendor Agreement - text lines + signature boxes
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(5.dp).background(Color(0xFF333333)))
                        Spacer(modifier = Modifier.height(3.dp))
                        repeat(6) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE0E0E0)))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color(0xFFF0F0F0)))
                            Box(modifier = Modifier.weight(1f).height(8.dp).background(Color(0xFFF0F0F0)))
                        }
                    }
                }
                name.contains("Report", ignoreCase = true) -> {
                    // Annual Report - corporate layout with blue graphic
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(Color(0xFF0073C6)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(4.dp).background(Color(0xFF555555)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(20.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF00B294)))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF2C811)))
                        }
                    }
                }
                name.contains("Manual", ignoreCase = true) -> {
                    // Product Manual - title + large center block
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).background(Color(0xFF333333)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.size(24.dp).background(Color(0xFFE53935), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.4f).height(3.dp).background(Color(0xFF888888)))
                    }
                }
                name.contains("Agendas", ignoreCase = true) -> {
                    // Team Meeting Agendas - lists/bullets
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).background(Color(0xFF0073C6)))
                        Spacer(modifier = Modifier.height(6.dp))
                        repeat(3) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(3.dp).background(Color(0xFF555555), CircleShape))
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).background(Color(0xFFE0E0E0)))
                            }
                        }
                    }
                }
                name.contains("Brief", ignoreCase = true) -> {
                    // Creative Brief - minimalist title
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Creative_Brief",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(2.dp).background(Color.Gray))
                    }
                }
                name.contains("Schedule", ignoreCase = true) -> {
                    // Event Schedule - table style grid
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(5.dp).background(Color(0xFF333333)))
                        Spacer(modifier = Modifier.height(4.dp))
                        // Table rows
                        repeat(4) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).height(5.dp).background(Color(0xFFF0F0F0)))
                                Box(modifier = Modifier.weight(2f).height(5.dp).background(Color(0xFFF9F9F9)))
                            }
                        }
                    }
                }
                else -> {
                    // Generic document page
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(0.6f).height(5.dp).background(Color(0xFF333333)))
                        Spacer(modifier = Modifier.height(4.dp))
                        repeat(8) {
                            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE0E0E0)))
                        }
                    }
                }
            }
        }
        
        // Bottom-right PDF Badge (black badge)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 6.dp, end = 6.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(2.dp))
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Small PDF sheet icon shape
                Canvas(modifier = Modifier.size(6.dp, 8.dp)) {
                    val strokeVal = 1.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.6f, 0f)
                        lineTo(w, h * 0.3f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = path, color = Color.White, style = Stroke(width = strokeVal))
                }
                Text(
                    text = "PDF",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// --- Home Screen UI ---

@Composable
fun HomeScreen(
    recentFiles: List<RecentFile>,
    onOpenFile: (RecentFile) -> Unit,
    onTriggerFilePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("Home") }
    var sortKey by remember { mutableStateOf("Recent") } // Recent, Name, Size
    var sortAscending by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var globalSearchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf("Grid") } // Grid, List
    var hideSuggestions by remember { mutableStateOf(false) }

    val darkBg = Color(0xFF1E1E1E)
    val panelBg = Color(0xFF171717)
    val textPrimary = Color(0xFFE5E5E5)
    val textSecondary = Color(0xFF999999)
    val blueAccent = Color(0xFF1473E6)
    val dividerColor = Color(0xFF2E2E2E)

    // Filter and Sort files
    val filteredFiles = remember(recentFiles, filterQuery, sortKey, sortAscending) {
        var list = recentFiles.filter { it.name.contains(filterQuery, ignoreCase = true) }
        
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
        list
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
                .padding(vertical = 12.dp, horizontal = 16.dp)
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

            // Blue Pill "New PDF" button
            Button(
                onClick = onTriggerFilePicker,
                colors = ButtonDefaults.buttonColors(containerColor = blueAccent),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "New PDF",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Outline "Open PDF" text link / ghost button
            OutlinedButton(
                onClick = onTriggerFilePicker,
                border = BorderStroke(1.dp, Color(0xFF555555)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Open PDF",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Sidebar Tabs
            val tabs = listOf(
                Pair("Home", @Composable { color: Color -> HomeIcon(color) }),
                Pair("Learn", @Composable { color: Color -> LearnIcon(color) })
            )
            
            tabs.forEach { (tabName, icon) ->
                val isActive = activeTab == tabName
                HoverableItem(
                    onClick = { activeTab = tabName },
                    normalColor = if (isActive) Color(0xFF2B2B2B) else Color.Transparent,
                    hoverColor = if (isActive) Color(0xFF2B2B2B) else Color(0xFF202020),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            // DOCUMENTS section (renamed from FILES)
            Text(
                text = "DOCUMENTS",
                color = textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            val fileSections = listOf(
                Pair("Your documents", @Composable { color: Color -> FolderIcon(color) }),
                Pair("Shared with me", @Composable { color: Color -> PeopleIcon(color) }),
                Pair("Adobe Document Cloud", @Composable { color: Color -> CloudIcon(color) }),
                Pair("Deleted", @Composable { color: Color -> TrashIcon(color) })
            )

            fileSections.forEach { (sectionName, icon) ->
                val isSelectedSection = activeTab == sectionName
                HoverableItem(
                    onClick = { activeTab = sectionName },
                    normalColor = if (isSelectedSection) Color(0xFF2B2B2B) else Color.Transparent,
                    hoverColor = if (isSelectedSection) Color(0xFF2B2B2B) else Color(0xFF202020),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        icon(if (isSelectedSection) Color.White else textSecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = sectionName,
                            color = if (isSelectedSection) Color.White else textSecondary,
                            fontWeight = if (isSelectedSection) FontWeight.Bold else FontWeight.Medium,
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
            // TOP BAR MENU (Adobe style with menus, global search, and actions)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(panelBg)
                    .padding(horizontal = 16.dp)
            ) {
                // Menus (File, Edit, View, Selector, Start, Window, Help)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val menus = listOf(
                        "File", "Edit", "View", "Selector", "Start", "Window", "Help"
                    )
                    menus.forEach { menu ->
                        var isHovered by remember { mutableStateOf(false) }
                        Text(
                            text = menu,
                            color = if (isHovered) Color.White else textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { }
                                .hoverable(remember { MutableInteractionSource() })
                                .onHover { isHovered = it }
                        )
                    }
                }

                // Global Search box inside the menu row itself
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(280.dp)
                        .height(26.dp)
                        .background(Color(0xFF222222), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp)
                ) {
                    SearchIcon(color = textSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = globalSearchQuery,
                        onValueChange = { globalSearchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = textPrimary, fontSize = 12.sp),
                        cursorBrush = SolidColor(blueAccent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (globalSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search",
                                    color = Color(0xFF666666),
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Action Icons (Cloud status, Notification, Profile avatar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                        CloudIcon(color = textSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                        GiftIcon(color = textSecondary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF888888))
                            .border(1.dp, Color(0xFF999999), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "YS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Workspace Panels depending on active tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                if (activeTab == "Learn") {
                    // --- LEARN SCREEN PANEL ---
                    Text(
                        text = "Learn LuminaPDF",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Quick interactive courses and guides to speed up your PDF productivity.",
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    val tutorials = listOf(
                        Triple("Organize PDF pages easily", "3 min • Page Editing Basics", "Learn how to reorder, extract, insert, or delete pages in a PDF document."),
                        Triple("Highlighting & markup structures", "5 min • Annotation Engine", "Unlock professional annotation systems: draw highlights, notes, and extract tabular data."),
                        Triple("Platform integrations in Compose Multiplatform", "8 min • Advanced", "See how the Expect/Actual systems interface native rendering drivers in KMP.")
                    )

                    tutorials.forEach { (title, subtitle, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF252525)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clickable { }
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(text = subtitle, color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = desc, color = textSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (activeTab == "Deleted") {
                    // --- DELETED SCREEN PANEL ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TrashIcon(color = textSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "No deleted files", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "Files you delete will show up here.", color = textSecondary, fontSize = 13.sp)
                        }
                    }
                } else if (activeTab != "Home") {
                    // --- GENERIC MOCK TABS PANEL ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FolderIcon(color = textSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Section Empty", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "This directory in $activeTab has no files.", color = textSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    // --- DEFAULT HOME SCREEN WORKSPACE ---
                    
                    // 2a. SUGGESTIONS HERO BANNER (CAROUSEL)
                    if (!hideSuggestions) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Suggestions",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                InfoIcon(color = textSecondary)
                            }
                            
                            Text(
                                text = "Hide suggestions",
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { hideSuggestions = true }
                                    .hoverable(remember { MutableInteractionSource() })
                            )
                        }

                        // Hero Banner Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .padding(bottom = 32.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Info pane on the left
                                Column(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Popular now",
                                            color = Color(0xFFFF9800),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Organize your PDF pages.",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Learn how to easily reorder, insert, delete, and combine pages in a PDF document.",
                                            color = textSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {},
                                            border = BorderStroke(1.dp, Color.White),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = "Watch tutorial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Image banner on the right
                                Box(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                ) {
                                    val heroPainter = painterResource(Res.drawable.suggestions_hero)
                                    Image(
                                        painter = heroPainter,
                                        contentDescription = "Tutorial Illustration",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Custom carousel arrows overlay
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("<", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(">", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2b. RECENT FILES LIST
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Recent",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sort & Filter Toolbar row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        // Left toolbar items: Sort dropdown & direction arrow
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var showSortDropdown by remember { mutableStateOf(false) }
                            Box {
                                TextButton(
                                    onClick = { showSortDropdown = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = textPrimary)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "Sort  $sortKey", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "v", fontSize = 10.sp, color = textSecondary)
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

                            IconButton(
                                onClick = { sortAscending = !sortAscending },
                                modifier = Modifier.size(28.dp)
                            ) {
                                SortArrowIcon(isAscending = sortAscending, color = textPrimary)
                            }
                        }

                        // Right toolbar items: Filter & View Switcher
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Filter input field
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(240.dp)
                                    .height(30.dp)
                                    .background(Color(0xFF252525), RoundedCornerShape(4.dp))
                                    .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp)
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
                                                text = "Filter Recent Files",
                                                color = Color(0xFF666666),
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            // View mode switcher: List / Grid
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF252525), RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            ) {
                                val isList = viewMode == "List"
                                val isGrid = viewMode == "Grid"
                                
                                IconButton(
                                    onClick = { viewMode = "List" },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (isList) Color(0xFF333333) else Color.Transparent, RoundedCornerShape(3.dp))
                                ) {
                                    ListIcon(color = if (isList) Color.White else textSecondary)
                                }
                                
                                IconButton(
                                    onClick = { viewMode = "Grid" },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (isGrid) Color(0xFF333333) else Color.Transparent, RoundedCornerShape(3.dp))
                                ) {
                                    GridIcon(color = if (isGrid) Color.White else textSecondary)
                                }
                            }
                        }
                    }

                    // Recent items Grid/List layout
                    if (filteredFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No recent files found", color = textSecondary, fontSize = 14.sp)
                        }
                    } else {
                        if (viewMode == "Grid") {
                            // GRID VIEW
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
                                                        onClick = { onOpenFile(fileItem) },
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
                            // LIST VIEW
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, dividerColor, RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1D1D1D))
                            ) {
                                // Table Header
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
                                    Spacer(modifier = Modifier.width(32.dp)) // space for cloud status
                                }

                                filteredFiles.forEachIndexed { i, fileItem ->
                                    if (i > 0) {
                                        HorizontalDivider(color = dividerColor, thickness = 1.dp)
                                    }
                                    ListItemRow(
                                        fileItem = fileItem,
                                        onClick = { onOpenFile(fileItem) },
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

// --- Component Card for Grid Item ---
@Composable
fun GridItemCard(
    fileItem: RecentFile,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF252525)
    
    HoverableItem(
        onClick = onClick,
        normalColor = cardBg,
        hoverColor = Color(0xFF2B2B2B),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
    ) { isHovered ->
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail container (aspect ratio 4:3 or standard height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF151515))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (fileItem.thumbnail != null) {
                    Image(
                        bitmap = fileItem.thumbnail,
                        contentDescription = fileItem.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                    
                    // PDF Badge Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 6.dp, end = 6.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(2.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Canvas(modifier = Modifier.size(6.dp, 8.dp)) {
                                val strokeVal = 1.dp.toPx()
                                val w = size.width
                                val h = size.height
                                val path = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(w * 0.6f, 0f)
                                    lineTo(w, h * 0.3f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(path = path, color = Color.White, style = Stroke(width = strokeVal))
                            }
                            Text(
                                text = "PDF",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    MockDocumentThumbnail(name = fileItem.name, modifier = Modifier.fillMaxSize())
                }

                // Green Sync checkmark (mock cloud synced state on bottom left)
                if (fileItem.isSample || fileItem.fileBytes != null) {
                    SyncCheckIcon(
                        color = Color(0xFF0F9D58),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            }

            // Text section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = fileItem.name,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fileItem.lastOpened,
                    color = textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// --- Component Row for List Item ---
@Composable
fun ListItemRow(
    fileItem: RecentFile,
    onClick: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    HoverableItem(
        onClick = onClick,
        normalColor = Color.Transparent,
        hoverColor = Color(0xFF252525),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) { isHovered ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // File Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // PDF small badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE53935), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = fileItem.name,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Last Opened timestamp
            Text(
                text = fileItem.lastOpened,
                color = textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.width(120.dp)
            )
            
            // File Size
            Text(
                text = fileItem.size,
                color = textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.width(80.dp)
            )

            // Cloud sync checkmark badge
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (fileItem.isSample || fileItem.fileBytes != null) {
                    SyncCheckIcon(color = Color(0xFF0F9D58))
                }
            }
        }
    }
}

// Custom Extension to detect hover safely in pure Multiplatform
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.onHover(action: (Boolean) -> Unit): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Enter -> action(true)
                PointerEventType.Exit -> action(false)
            }
        }
    }
}
