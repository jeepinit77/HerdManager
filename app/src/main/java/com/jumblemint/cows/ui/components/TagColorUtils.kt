package com.jumblemint.cows.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.jumblemint.cows.data.model.TagColor
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow

/**
 * Utility composable that provides a map of tag color names to their actual Color values
 * from the TagColor database table.
 */
@Composable
fun rememberTagColorMap(repository: CattleRepository): Map<String, Color> {
    val tagColors by repository.getAllActiveTagColors().collectAsState(initial = emptyList())
    return remember(tagColors) {
        tagColors.associate { it.name.lowercase() to it.toColor() }
    }
}

/**
 * Resolves a tag color name to its actual Color value from the TagColor database.
 * Falls back to predefined colors if not found in database.
 */
fun resolveTagColor(tagColorName: String?, tagColorMap: Map<String, Color>): Color? {
    if (tagColorName == null) return null
    
    // First try to get from database
    tagColorMap[tagColorName.lowercase()]?.let { return it }
    
    // Fallback to predefined colors for backward compatibility
    return when (tagColorName.lowercase()) {
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
        else -> null
    }
}

/**
 * Composable that provides a resolved tag color for a given tag color name.
 * Returns null if no color is found.
 */
@Composable
fun rememberResolvedTagColor(tagColorName: String?, repository: CattleRepository): Color? {
    val tagColorMap = rememberTagColorMap(repository)
    return remember(tagColorName, tagColorMap) {
        resolveTagColor(tagColorName, tagColorMap)
    }
}