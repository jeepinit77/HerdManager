package com.jumblemint.cows.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import com.jumblemint.cows.ui.theme.getGenderColor
import com.jumblemint.cows.ui.theme.getCardBackgroundColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons // Added this import
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import com.jumblemint.cows.ui.theme.contrastingTextColor
import com.jumblemint.cows.ui.theme.BackgroundColorProvider
import com.jumblemint.cows.ui.theme.SmartText
// import androidx.compose.material.icons.filled.Lightbulb // Removed
import com.jumblemint.cows.R // Assuming your R file is here
import com.jumblemint.cows.data.model.Cow
import com.jumblemint.cows.data.model.Gender
import com.jumblemint.cows.data.model.Status
import java.time.LocalDate
import java.time.Period
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.unit.Dp
import com.jumblemint.cows.ui.components.WobblingLightbulbIcon // Changed import
import com.jumblemint.cows.ui.theme.SmartText

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
    val genderColor = getGenderColor(cow.gender)
    val cardColors = CardDefaults.cardColors(
        containerColor = genderColor
    )


    val context = LocalContext.current
    val tipsManager = remember { com.jumblemint.cows.data.preferences.TipsManager(context) }
    val tipId = "cow_card_watch_tip"
    val tipVisible by tipsManager.isTipVisible(tipId).collectAsState(initial = false)
    var showTip by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) { // Outer Box for positioning the icon relative to the card
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(), // Card fills the Box
            shape = MaterialTheme.shapes.medium,
            colors = cardColors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            BackgroundColorProvider(backgroundColor = genderColor) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (cow.tagNumber != null || cow.tagColor != null) {
                        CattleTagBadge(
                            tagNumber = cow.tagNumber,
                            tagColor = cow.tagColor,
                            modifier = Modifier.size(width = 72.dp, height = 96.dp),
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
                                SmartText(
                                    text = cow.name ?: "Unnamed Cow",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                SmartText(
                                    text = cow.classification.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // Action buttons (Star, Edit, Delete) - Tip icon is removed from here
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                onToggleWatch?.let {
                                    IconButton(onClick = it) {
                                        val tint = if (cow.isWatched) MaterialTheme.colorScheme.tertiary else genderColor.contrastingTextColor().copy(alpha = 0.7f)
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = if (cow.isWatched) "Unwatch" else "Watch",
                                            tint = tint
                                        )
                                    }
                                }
                                onEdit?.let {
                                    IconButton(onClick = it) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Cow",
                                            tint = genderColor.contrastingTextColor()
                                        )
                                    }
                                }
                                onDelete?.let {
                                    IconButton(onClick = it) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Cow",
                                            tint = if (genderColor.luminance() < 0.5f) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }

                        cow.birthDate?.let { birthDate ->
                            val age = Period.between(birthDate, LocalDate.now())
                            val ageText = when {
                                age.years > 0 -> "${age.years}y ${age.months}m"
                                age.months > 0 -> "${age.months}m ${age.days}d"
                                else -> "${age.days}d"
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            SmartText(
                                text = "Age: $ageText",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } // End of BackgroundColorProvider
        } // End of Card content

        // Tip Icon Button - overlaid on top-right of the Card, extending out
        if (tipVisible) {
            IconButton(
                onClick = { showTip = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-18).dp) // Offset to extend past the edge
            ) {
                WobblingLightbulbIcon() // Changed to WobblingLightbulbIcon
            }
        }

        // TipOverlay Dialog - its position in the tree here doesn't affect its modal appearance
        if (showTip) {
            TipOverlay(
                tipId = tipId,
                tipText = "Tap the star to mark a cow as watched. Watched cows appear at the top of lists.",
                onClosed = { showTip = false },
                tipsManager = tipsManager
            )
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

@Composable
fun CattleTagBadge(tagNumber: String?, tagColor: String?, modifier: Modifier = Modifier, backgroundColor: Color? = null) {
    val bgColor = backgroundColor ?: when (tagColor?.lowercase()) {
        "red" -> Color.Red
        "blue" -> Color.Blue
        "green" -> Color.Green
        "yellow" -> Color.Yellow
        "orange" -> Color(0xFFFFA500)
        "purple" -> Color.Magenta
        "pink" -> Color(0xFFFFC0CB)
        "white" -> Color.White
        "black" -> Color.Black
        "brown" -> Color(0xFF8B4513)
        else -> MaterialTheme.colorScheme.surfaceVariant // Default color if tagColor is null or not recognized
    }

    Box(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ear_tag),
            contentDescription = "Cattle Tag",
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = R.drawable.ear_tag),
            contentDescription = "Cattle Tag",
            colorFilter = ColorFilter.tint(bgColor),
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
        )
        BackgroundColorProvider(backgroundColor = bgColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
            tagColor?.let {
                SmartText(
                    text = it.lowercase().replaceFirstChar { char -> char.titlecase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    backgroundColor = bgColor
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            SmartText(
                text = (tagNumber ?: "—").uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                backgroundColor = bgColor
            )
            }
        } // End of BackgroundColorProvider
    }
}
