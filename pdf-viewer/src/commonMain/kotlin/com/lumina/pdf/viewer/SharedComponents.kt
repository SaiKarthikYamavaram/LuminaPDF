package com.lumina.pdf.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.pdf.viewer.theme.LuminaRadius
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Canvas Icons for Sleek, Dependency-free Rendering ---

@Composable
fun AppLogoMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier.size(32.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        // Folded-corner document mark, drawn directly on the surrounding surface (no badge
        // background) so it reads as part of the Paper & Ink palette rather than a fixed chip.
        val left = w * 0.30f
        val right = w * 0.70f
        val top = h * 0.22f
        val bottom = h * 0.78f
        val fold = (right - left) * 0.34f

        val page = Path().apply {
            moveTo(left, top)
            lineTo(right - fold, top)
            lineTo(right, top + fold)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(path = page, color = color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val foldPath = Path().apply {
            moveTo(right - fold, top)
            lineTo(right - fold, top + fold)
            lineTo(right, top + fold)
        }
        drawPath(path = foldPath, color = color, style = Stroke(width = stroke * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Text lines
        val lineStroke = stroke * 0.65f
        val lineLeft = left + (right - left) * 0.18f
        val lineRightShort = right - (right - left) * 0.28f
        val lineRightLong = right - (right - left) * 0.14f
        val lineSpacing = (bottom - top - fold) / 4f
        var y = top + fold + lineSpacing
        drawLine(color, Offset(lineLeft, y), Offset(lineRightLong, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
        y += lineSpacing
        drawLine(color, Offset(lineLeft, y), Offset(lineRightShort, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
        y += lineSpacing
        drawLine(color, Offset(lineLeft, y), Offset(lineRightLong, y), strokeWidth = lineStroke, cap = StrokeCap.Round)
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
        val r = size.minDimension / 4f
        val cx = size.width * 0.45f
        val cy = size.height * 0.45f
        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = stroke)
        )
        val startX = (cx + r * cos(PI/4)).toFloat()
        val startY = (cy + r * sin(PI/4)).toFloat()
        val endX = size.width * 0.8f
        val endY = size.height * 0.8f
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

@Composable
fun AddIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.3f
        val stroke = 2.dp.toPx()
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx, cy - len), end = Offset(cx, cy + len), strokeWidth = stroke)
    }
}

@Composable
fun RemoveIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.3f
        val stroke = 2.dp.toPx()
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = stroke)
    }
}

@Composable
fun MoreVertIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val r = size.minDimension * 0.08f
        val gap = size.height * 0.28f
        val cy = size.height / 2f
        drawCircle(color = color, radius = r, center = Offset(cx, cy - gap))
        drawCircle(color = color, radius = r, center = Offset(cx, cy))
        drawCircle(color = color, radius = r, center = Offset(cx, cy + gap))
    }
}

@Composable
fun SunIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val r = size.minDimension / 4f
        drawCircle(color = color, radius = r)
        val center = center
        val rayLength = size.minDimension / 6f
        for (i in 0 until 8) {
            val angle = i * (2.0 * PI / 8.0)
            val startX = (center.x + r * cos(angle)).toFloat()
            val startY = (center.y + r * sin(angle)).toFloat()
            val endX = (center.x + (r + rayLength) * cos(angle)).toFloat()
            val endY = (center.y + (r + rayLength) * sin(angle)).toFloat()
            drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
fun MoonIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val radius = size.minDimension / 3f
        val path = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
        }
        val subPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = center - Offset(6f, 3f), radius = radius))
        }
        val moonPath = Path.combine(
            PathOperation.Difference,
            path,
            subPath
        )
        drawPath(path = moonPath, color = color)
    }
}

@Composable
fun ArrowBackIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.25f
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx - len + len * 0.7f, cy - len * 0.7f), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx - len + len * 0.7f, cy + len * 0.7f), strokeWidth = stroke)
    }
}

@Composable
fun ChevronUpIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.2f
        val path = Path().apply {
            moveTo(cx - len, cy + len / 2f)
            lineTo(cx, cy - len / 2f)
            lineTo(cx + len, cy + len / 2f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke))
    }
}

@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.2f
        val path = Path().apply {
            moveTo(cx - len, cy - len / 2f)
            lineTo(cx, cy + len / 2f)
            lineTo(cx + len, cy - len / 2f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke))
    }
}

@Composable
fun MenuIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val startX = w * 0.15f
        val endX = w * 0.85f
        
        val strokeWidth = 2.dp.toPx()
        drawLine(color = color, start = Offset(startX, h * 0.25f), end = Offset(endX, h * 0.25f), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(startX, h * 0.50f), end = Offset(endX, h * 0.50f), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(startX, h * 0.75f), end = Offset(endX, h * 0.75f), strokeWidth = strokeWidth)
    }
}

@Composable
fun NoteIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            lineTo(w * 0.6f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.4f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.85f)
            close()
            // Folded corner
            moveTo(w * 0.6f, h * 0.15f)
            lineTo(w * 0.6f, h * 0.4f)
            lineTo(w * 0.85f, h * 0.4f)
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke))
    }
}

@Composable
fun BookmarkIcon(color: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.1f)
            lineTo(w * 0.75f, h * 0.1f)
            lineTo(w * 0.75f, h * 0.9f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.25f, h * 0.9f)
            close()
        }
        if (filled) {
            drawPath(path = path, color = color)
        } else {
            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
        }
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
    // clickable() and hoverable() must share ONE interactionSource - passing none to clickable()
    // (as this did before) makes it create its own internal, uncoordinated source, so the press
    // state driving the actual click and the hover state driving isHovered/this background could
    // desync (e.g. a hover left stuck true from a touch that Compose synthesizes hover events for
    // without a clean exit) instead of reflecting one consistent interaction.
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val indication = LocalIndication.current

    Box(
        modifier = modifier
            .background(if (isHovered) hoverColor else normalColor, shape)
            // Merges this row's child Text/Icon nodes into one accessible node on the
            // clickable itself, instead of TalkBack/VoiceOver traversing an unlabeled
            // "button" and its text children as separate, disconnected stops.
            .semantics(mergeDescendants = true) {}
            .clickable(interactionSource = interactionSource, indication = indication, onClick = onClick)
            .hoverable(interactionSource),
        content = { content(isHovered) }
    )
}

// --- Canvas Mock Document Page Preview ---
@Composable
fun MockDocumentThumbnail(name: String, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // White page container — the surrounding parent already supplies the sunken
        // theme-aware background, so this frame stays transparent instead of overriding it.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f / 1.414f) // standard A4 aspect ratio
                .background(colorScheme.surfaceContainerLow, RoundedCornerShape(LuminaRadius.sm))
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(LuminaRadius.sm))
                .padding(8.dp)
        ) {
            // Generic "no thumbnail yet" placeholder — deliberately does not infer or
            // fabricate document content from the filename.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(5.dp).background(colorScheme.onSurface))
                Spacer(modifier = Modifier.height(4.dp))
                repeat(8) {
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(colorScheme.outlineVariant))
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
