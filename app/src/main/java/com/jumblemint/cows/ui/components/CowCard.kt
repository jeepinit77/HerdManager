package com.jumblemint.cows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import java.time.LocalDate
import java.time.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowCard(
    cow: Cow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWatch: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    // Gender-based background colors - more distinct and noticeable, theme-aware
    val isDarkTheme = isSystemInDarkTheme()
    val cardColors = when (cow.gender) {
        Gender.FEMALE -> CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF8E2A5B) else Color(0xFFFFB3D9) // Dark pink / Bright pink
        )
        Gender.MALE -> CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2A5B8E) else Color(0xFFB3D9FF) // Dark blue / Bright blue
        )
        Gender.TBD -> CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF4A4A4A) else Color(0xFFE0E0E0) // Dark gray / Medium gray
        )
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Tag on the top-left
                if (cow.tagNumber != null || cow.tagColor != null) {
                    CattleTagBadge(
                        tagNumber = cow.tagNumber,
                        tagColor = cow.tagColor,
                        modifier = Modifier.height(64.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cow.name ?: "Unnamed Cow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cow.classification.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Watch toggle + Edit + Delete buttons on the right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            onToggleWatch?.let {
                                IconButton(onClick = it) {
                                    val tint = if (cow.isWatched) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Star,
                                        contentDescription = if (cow.isWatched) "Unwatch" else "Watch",
                                        tint = tint
                                    )
                                }
                            }
                            onEdit?.let {
                                IconButton(onClick = it) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                                        contentDescription = "Edit Cow"
                                    )
                                }
                            }
                            onDelete?.let {
                                IconButton(onClick = it) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                                        contentDescription = "Delete Cow",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    cow.birthDate?.let { birthDate ->
                        val age = Period.between(birthDate, LocalDate.now())
                        val ageText = when {
                            age.years > 0 -> "${age.years}y ${age.months}m"
                            age.months > 0 -> "${age.months}m ${age.days}d"
                            else -> "${age.days}d"
                        }
                        Text(
                            text = "Age: $ageText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            

        }
    }
}

@Composable
fun StatusBadge(status: Status) {
    val (color, text) = when (status) {
        Status.ACTIVE -> MaterialTheme.colorScheme.primary to "Active"
        Status.SOLD -> MaterialTheme.colorScheme.secondary to "Sold"
        Status.DECEASED -> MaterialTheme.colorScheme.error to "Deceased"
    }
    
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Custom cattle ear tag with a rounded dome top and a small cutout hole
class CattleTagShape(
    private val cornerRadius: Float = 16f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { fillType = PathFillType.EvenOdd }
        val w = size.width
        val h = size.height

        val left = 0f
        val top = 0f
        val right = w
        val bottom = h
        val cx = w / 2f

        // Geometry for the rounded dome and short straight sides near the top
        val bodyTopY = h * 0.35f           // Where the main body starts (below the dome)
        val straightSide = h * 0.06f       // Short straight segment before curving into the dome

        // Outer tag path (clockwise)
        path.moveTo(left + cornerRadius, bottom)
        // Bottom edge to bottom-right
        path.lineTo(right - cornerRadius, bottom)
        // Bottom-right corner
        path.quadraticBezierTo(right, bottom, right, bottom - cornerRadius)
        // Right edge up to short straight segment below the dome
        path.lineTo(right, bodyTopY + straightSide)
        // Short straight segment
        path.lineTo(right, bodyTopY)
        // Rounded dome: right shoulder to apex
        path.quadraticBezierTo(right, top, cx, top)
        // Rounded dome: apex to left shoulder
        path.quadraticBezierTo(left, top, left, bodyTopY)
        // Short straight segment on the left
        path.lineTo(left, bodyTopY + straightSide)
        // Left edge down to rounded bottom-left
        path.lineTo(left, bottom - cornerRadius)
        // Bottom-left corner
        path.quadraticBezierTo(left, bottom, left + cornerRadius, bottom)
        path.close()

        // Small round cutout hole near the top center
        val holeRadius = with(density) { 4.dp.toPx() }
        val holeCenterY = (h * 0.12f).coerceAtLeast(holeRadius + 1f)
        val holeRect = Rect(cx - holeRadius, holeCenterY - holeRadius, cx + holeRadius, holeCenterY + holeRadius)
        path.addOval(holeRect)

        return Outline.Generic(path)
    }
}

@Composable
fun CattleTagBadge(tagNumber: String?, tagColor: String?, modifier: Modifier = Modifier) {
    // Fallbacks
    val bgColor = when (tagColor?.lowercase()) {
        "red" -> androidx.compose.ui.graphics.Color.Red
        "blue" -> androidx.compose.ui.graphics.Color.Blue
        "green" -> androidx.compose.ui.graphics.Color.Green
        "yellow" -> androidx.compose.ui.graphics.Color.Yellow
        "orange" -> androidx.compose.ui.graphics.Color(0xFFFFA500)
        "purple" -> androidx.compose.ui.graphics.Color.Magenta
        "pink" -> androidx.compose.ui.graphics.Color(0xFFFFC0CB)
        "white" -> androidx.compose.ui.graphics.Color.White
        "black" -> androidx.compose.ui.graphics.Color.Black
        "brown" -> androidx.compose.ui.graphics.Color(0xFF8B4513)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = bgColor,
        shape = CattleTagShape(),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 36.dp)
                .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = (tagNumber ?: "—").uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                color = if (bgColor.luminance() < 0.5f) Color.White else Color.Black
            )
            tagColor?.let { name ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = name.lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (bgColor.luminance() < 0.5f) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}