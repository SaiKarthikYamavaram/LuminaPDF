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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.pdf.viewer.theme.LuminaRadius
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- Icons ---
//
// Every icon is drawn on one 24x24 unit grid with a ~2-unit keyline margin, a single stroke
// weight and round caps and joins, so the set reads as one family (each icon used to pick its
// own weight between 1.5dp and 2dp, its own extents and its own caps). The weight scales with
// the icon, so a 14dp badge and a 32dp logo keep the same optical density.
//
// Coordinates below are grid units: the draw lambda gets `u`, one unit in pixels.

private const val IconGrid = 24f

/** Stroke weight in grid units, shared by the whole set. */
private const val IconWeight = 1.8f

/** Default drawn size. Callers can still pass a smaller `Modifier.size(...)`, which wins. */
private val IconSizeDefault = 20.dp

@Composable
private fun IconCanvas(
    modifier: Modifier,
    size: Dp = IconSizeDefault,
    weight: Float = IconWeight,
    draw: DrawScope.(u: Float, stroke: Stroke) -> Unit
) {
    Canvas(modifier = modifier.size(size)) {
        val u = this.size.minDimension / IconGrid
        draw(u, Stroke(width = weight * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Open path through alternating x/y grid coordinates. */
private fun polyline(u: Float, vararg xy: Float): Path = Path().apply {
    moveTo(xy[0] * u, xy[1] * u)
    for (i in 2 until xy.size step 2) lineTo(xy[i] * u, xy[i + 1] * u)
}

/** Closed path through alternating x/y grid coordinates. */
private fun polygon(u: Float, vararg xy: Float): Path = polyline(u, *xy).apply { close() }

private fun DrawScope.gridLine(color: Color, u: Float, stroke: Stroke, x1: Float, y1: Float, x2: Float, y2: Float) {
    drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), stroke.width, StrokeCap.Round)
}

/**
 * The app mark: the page-and-L from the launcher icon (see design/lumina-icon.svg), drawn in a
 * single color so it sits on any surface.
 */
@Composable
fun AppLogoMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) =
    IconCanvas(modifier, size = 32.dp) { u, stroke ->
        drawPath(polygon(u, 5f, 3f, 14f, 3f, 19f, 8f, 19f, 21f, 5f, 21f), color, style = stroke)
        drawPath(polyline(u, 14f, 3f, 14f, 8f, 19f, 8f), color, style = stroke)
        drawPath(
            polyline(u, 9.5f, 9.5f, 9.5f, 16.5f, 15f, 16.5f),
            color,
            style = Stroke(width = 2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

@Composable
fun FolderIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawPath(polygon(u, 3.5f, 19.5f, 3.5f, 5f, 9.4f, 5f, 11.6f, 7.6f, 20.5f, 7.6f, 20.5f, 19.5f), color, style = stroke)
}

@Composable
fun SearchIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawCircle(color, radius = 6.2f * u, center = Offset(10.4f * u, 10.4f * u), style = stroke)
    gridLine(color, u, stroke, 14.9f, 14.9f, 20.3f, 20.3f)
}

@Composable
fun GridIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier, size = 18.dp) { u, stroke ->
    for (x in listOf(4f, 13f)) {
        for (y in listOf(4f, 13f)) {
            drawRoundRect(
                color,
                topLeft = Offset(x * u, y * u),
                size = Size(7f * u, 7f * u),
                cornerRadius = CornerRadius(1.6f * u),
                style = stroke
            )
        }
    }
}

@Composable
fun ListIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier, size = 18.dp) { u, stroke ->
    for (y in listOf(6.5f, 12f, 17.5f)) {
        drawCircle(color, radius = 1.2f * u, center = Offset(5f * u, y * u))
        gridLine(color, u, stroke, 9f, y, 19.5f, y)
    }
}

@Composable
fun SortArrowIcon(isAscending: Boolean, color: Color, modifier: Modifier = Modifier) =
    IconCanvas(modifier, size = 16.dp) { u, stroke ->
        gridLine(color, u, stroke, 12f, 4.2f, 12f, 19.8f)
        val head = if (isAscending) {
            polyline(u, 7.8f, 8.4f, 12f, 4.2f, 16.2f, 8.4f)
        } else {
            polyline(u, 7.8f, 15.6f, 12f, 19.8f, 16.2f, 15.6f)
        }
        drawPath(head, color, style = stroke)
    }

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    gridLine(color, u, stroke, 6.8f, 6.8f, 17.2f, 17.2f)
    gridLine(color, u, stroke, 17.2f, 6.8f, 6.8f, 17.2f)
}

@Composable
fun NotesIndicatorIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier, size = 16.dp) { u, stroke ->
    drawPath(
        polygon(u, 3.5f, 4.6f, 20.5f, 4.6f, 20.5f, 15f, 10.4f, 15f, 7f, 18.8f, 7f, 15f, 3.5f, 15f),
        color,
        style = stroke
    )
    gridLine(color, u, stroke, 6.9f, 8f, 17.1f, 8f)
    gridLine(color, u, stroke, 6.9f, 11.4f, 14f, 11.4f)
}

@Composable
fun ArrowRightIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier, size = 16.dp) { u, stroke ->
    drawPath(polyline(u, 9.6f, 5.6f, 16f, 12f, 9.6f, 18.4f), color, style = stroke)
}

@Composable
fun AddIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    gridLine(color, u, stroke, 12f, 5.5f, 12f, 18.5f)
    gridLine(color, u, stroke, 5.5f, 12f, 18.5f, 12f)
}

@Composable
fun RemoveIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    gridLine(color, u, stroke, 5.5f, 12f, 18.5f, 12f)
}

@Composable
fun MoreVertIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, _ ->
    for (y in listOf(5.6f, 12f, 18.4f)) {
        drawCircle(color, radius = 1.55f * u, center = Offset(12f * u, y * u))
    }
}

@Composable
fun SunIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawCircle(color, radius = 4.3f * u, center = Offset(12f * u, 12f * u))
    for (i in 0 until 8) {
        val angle = i * (2.0 * PI / 8.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        gridLine(color, u, stroke, 12f + 6.4f * dx, 12f + 6.4f * dy, 12f + 9.4f * dx, 12f + 9.4f * dy)
    }
}

@Composable
fun MoonIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, _ ->
    val disc = Path().apply { addOval(Rect(center = Offset(12f * u, 12f * u), radius = 8.6f * u)) }
    val bite = Path().apply { addOval(Rect(center = Offset(6.6f * u, 8.2f * u), radius = 8.6f * u)) }
    drawPath(Path.combine(PathOperation.Difference, disc, bite), color)
}

@Composable
fun ArrowBackIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    gridLine(color, u, stroke, 4.6f, 12f, 19.4f, 12f)
    drawPath(polyline(u, 10.4f, 6.2f, 4.6f, 12f, 10.4f, 17.8f), color, style = stroke)
}

@Composable
fun ChevronUpIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawPath(polyline(u, 6.6f, 14.6f, 12f, 9.2f, 17.4f, 14.6f), color, style = stroke)
}

@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawPath(polyline(u, 6.6f, 9.4f, 12f, 14.8f, 17.4f, 9.4f), color, style = stroke)
}

@Composable
fun MenuIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    for (y in listOf(6.6f, 12f, 17.4f)) {
        gridLine(color, u, stroke, 4f, y, 20f, y)
    }
}

@Composable
fun NoteIcon(color: Color, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    drawPath(polygon(u, 5f, 3f, 14f, 3f, 19f, 8f, 19f, 21f, 5f, 21f), color, style = stroke)
    drawPath(polyline(u, 14f, 3f, 14f, 8f, 19f, 8f), color, style = stroke)
    gridLine(color, u, stroke, 8.2f, 12.6f, 15.8f, 12.6f)
    gridLine(color, u, stroke, 8.2f, 16.4f, 13.4f, 16.4f)
}

@Composable
fun BookmarkIcon(color: Color, filled: Boolean, modifier: Modifier = Modifier) = IconCanvas(modifier) { u, stroke ->
    val mark = polygon(u, 6.8f, 3.4f, 17.2f, 3.4f, 17.2f, 20.6f, 12f, 16.4f, 6.8f, 20.6f)
    if (filled) drawPath(mark, color) else drawPath(mark, color, style = stroke)
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
