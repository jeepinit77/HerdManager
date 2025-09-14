package com.jumblemint.cows.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import com.jumblemint.cows.ui.theme.getGenderColor
import com.jumblemint.cows.ui.theme.getCardBackgroundColor
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
import androidx.compose.ui.graphics.ColorFilter // Added this import
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.painterResource // Added this import
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jumblemint.cows.R // Assuming your R file is here
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import java.time.LocalDate
import java.time.Period
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CowCard(
    cow: Cow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleWatch: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    resolvedTagColor: Color? = null
) {
    // Use gender color as background for cow cards
    val genderColor = getGenderColor(cow.gender)
    val cardColors = CardDefaults.cardColors(
        containerColor = genderColor
    )
    
    // Calculate text color based on background luminance
    val textColor = if (genderColor.luminance() < 0.5f) Color.White else Color.Black
    val secondaryTextColor = if (genderColor.luminance() < 0.5f) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        modifier = Modifier.size(width = 72.dp, height = 96.dp), // Adjust size as needed for your SVG
                        backgroundColor = resolvedTagColor
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
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = cow.classification.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = secondaryTextColor
                            )
                        }
                        // Watch toggle + Edit + Delete buttons on the right
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            onToggleWatch?.let {
                                IconButton(onClick = it) {
                                    val tint = if (cow.isWatched) MaterialTheme.colorScheme.tertiary else secondaryTextColor
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
                                        contentDescription = "Edit Cow",
                                        tint = textColor
                                    )
                                }
                            }
                            onDelete?.let {
                                IconButton(onClick = it) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                                        contentDescription = "Delete Cow",
                                        tint = if (genderColor.luminance() < 0.5f) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
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
                            color = secondaryTextColor
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

// CattleTagShape class has been removed

@Composable
fun CattleTagBadge(tagNumber: String?, tagColor: String?, modifier: Modifier = Modifier, backgroundColor: Color? = null) {
    val bgColor = backgroundColor ?: when (tagColor?.lowercase()) {
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
        else -> MaterialTheme.colorScheme.surfaceVariant // Default color if tagColor is null or not recognized
    }

    Box(
        modifier = modifier,
        // contentAlignment = Alignment.Center // No longer Center for the Box itself
    ) {
        Image(
            painter = painterResource(id = R.drawable.ear_tag),
            contentDescription = "Cattle Tag",
            colorFilter = ColorFilter.tint(bgColor),
            modifier = Modifier.fillMaxSize() // SVG will fill the Box
        )
        Column(
            modifier = Modifier
                .fillMaxSize() // Column takes the full size of the Box
                .padding(horizontal = 4.dp, vertical = 8.dp), // General padding, adjust as needed
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom // Push content to the bottom
        ) {
            // Tag Color Text (appears above Tag Number)
            tagColor?.let { name ->
                Text(
                    text = name.lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelSmall, // Consider adjusting style
                    color = if (bgColor.luminance() < 0.5f) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(2.dp)) // Space between color and number
            }
            // Tag Number Text (at the very bottom)
            Text(
                text = (tagNumber ?: "—").uppercase(),
                style = MaterialTheme.typography.headlineSmall, // Changed to a larger style
                fontWeight = FontWeight.Bold, // Remains Bold
                maxLines = 1,
                softWrap = false,
                color = if (bgColor.luminance() < 0.5f) Color.White else Color.Black
            )
        }
    }
}