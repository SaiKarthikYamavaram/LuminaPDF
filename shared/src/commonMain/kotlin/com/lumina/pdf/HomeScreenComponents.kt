package com.lumina.pdf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumina.pdf.viewer.*
import com.lumina.pdf.viewer.theme.LuminaRadius
import com.lumina.pdf.viewer.theme.luminaDangerColor
import com.lumina.pdf.viewer.theme.luminaNotesColor

// --- Component Card for Grid Item ---
@Composable
fun GridItemCard(
    fileItem: RecentFile,
    hasNotes: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardBg = colorScheme.surfaceContainerLow
    val cardShape = RoundedCornerShape(LuminaRadius.lg)
    val notesColor = luminaNotesColor()

    HoverableItem(
        onClick = onClick,
        normalColor = cardBg,
        hoverColor = colorScheme.surfaceContainerHighest,
        shape = cardShape,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colorScheme.outlineVariant, cardShape)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail container (aspect ratio 4:3 or standard height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    // clip MUST come before background - a modifier only clips what's drawn by
                    // modifiers after it in the chain, so background() here was painting a full
                    // square rectangle that the later clip() couldn't retroactively round off,
                    // poking past the card's rounded border at the top two corners.
                    .clip(RoundedCornerShape(topStart = LuminaRadius.lg, topEnd = LuminaRadius.lg))
                    .background(colorScheme.surfaceContainerLowest),
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
                    
                    // PDF Badge Overlay — fixed dark scrim over the thumbnail image, intentionally
                    // theme-independent since it always sits on a rendered PDF page preview.
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

                // Always visible (not hover-gated) - hover has no equivalent on touch, so gating
                // this behind isHovered left no way to remove an item on Android at all.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClickLabel = "Remove ${fileItem.name} from recents") { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    CloseIcon(color = Color.White, modifier = Modifier.size(10.dp))
                }

                if (hasNotes) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        NotesIndicatorIcon(color = notesColor, modifier = Modifier.size(12.dp))
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
    hasNotes: Boolean,
    showLastOpened: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val notesColor = luminaNotesColor()
    val dangerColor = luminaDangerColor()

    HoverableItem(
        onClick = onClick,
        normalColor = Color.Transparent,
        hoverColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
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
                Box(
                    modifier = Modifier
                        .background(dangerColor, RoundedCornerShape(2.dp))
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
                    NotesIndicatorIcon(color = notesColor, modifier = Modifier.size(14.dp))
                }
            }

            // Last Opened timestamp - dropped on phone-width screens, see showLastOpened's
            // caller for why.
            if (showLastOpened) {
                Text(
                    text = fileItem.lastOpened,
                    color = textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.width(120.dp)
                )
            }

            // File Size
            Text(
                text = fileItem.size,
                color = textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.width(80.dp)
            )

            // Remove button - always visible (not hover-gated); hover has no equivalent on
            // touch, so gating this behind isHovered left no way to remove an item on Android.
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colorScheme.onSurface.copy(alpha = 0.1f))
                        .clickable(onClickLabel = "Remove ${fileItem.name} from recents") { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    CloseIcon(color = textSecondary, modifier = Modifier.size(10.dp))
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
