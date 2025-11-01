package com.jumblemint.cows.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun WobblingLightbulbIcon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "lightbulbWobble")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f, // Wobble from -15 degrees
        targetValue = 15f,  // Wobble to 15 degrees
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing), // Slow wobble
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightbulbRotation"
    )

    Box(
        modifier = modifier
            .size(28.dp) // Overall size of the component including background
            .background(Color.DarkGray, CircleShape) // Changed background to DarkGray
            .border(1.dp, Color.Black, CircleShape),
        contentAlignment = Alignment.Center // Center the Icon within the Box
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = "Tips Lightbulb",
            tint = Color.Yellow,
            modifier = Modifier
                .size(20.dp) // Icon itself is smaller to fit in the background
                .graphicsLayer {
                    rotationZ = rotation // Apply wobble animation to the icon
                }
        )
    }
}
