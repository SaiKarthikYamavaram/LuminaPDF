package com.lumina.pdf

import androidx.compose.animation.*
import androidx.compose.foundation.*
import com.lumina.pdf.viewer.db.rememberPdfDatabase
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

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val len = size.minDimension * 0.25f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(color = color, start = Offset(cx - len, cy - len), end = Offset(cx + len, cy + len), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx - len, cy + len), end = Offset(cx + len, cy - len), strokeWidth = stroke)
    }
}

@Composable
fun NotesIndicatorIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        
        // Chat bubble shape
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.65f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.3f, h * 0.85f)
            lineTo(w * 0.3f, h * 0.65f)
            lineTo(w * 0.15f, h * 0.65f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))
        
        // Horizontal lines inside bubble
        drawLine(color = color, start = Offset(w * 0.32f, h * 0.35f), end = Offset(w * 0.68f, h * 0.35f), strokeWidth = stroke)
        drawLine(color = color, start = Offset(w * 0.32f, h * 0.5f), end = Offset(w * 0.58f, h * 0.5f), strokeWidth = stroke)
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

@Composable
fun ArrowRightIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.65f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.75f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

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
                    e.printStackTrace()
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

// --- Component Card for Grid Item ---
@Composable
fun GridItemCard(
    fileItem: RecentFile,
    db: com.lumina.pdf.viewer.db.PdfDatabase,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF252525)
    val hasNotes by produceState(initialValue = false, key1 = fileItem.id) {
        value = db.getNotes(fileItem.id).isNotEmpty()
    }
    
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

                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        CloseIcon(color = Color.White, modifier = Modifier.size(10.dp))
                    }
                }

                if (hasNotes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        NotesIndicatorIcon(color = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                    }
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
    db: com.lumina.pdf.viewer.db.PdfDatabase,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val hasNotes by produceState(initialValue = false, key1 = fileItem.id) {
        value = db.getNotes(fileItem.id).isNotEmpty()
    }
    
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasNotes) {
                    Spacer(modifier = Modifier.width(6.dp))
                    NotesIndicatorIcon(color = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                }
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
                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        CloseIcon(color = textSecondary, modifier = Modifier.size(10.dp))
                    }
                } else {
                    if (fileItem.isSample || fileItem.fileBytes != null) {
                        SyncCheckIcon(color = Color(0xFF0F9D58))
                    }
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
