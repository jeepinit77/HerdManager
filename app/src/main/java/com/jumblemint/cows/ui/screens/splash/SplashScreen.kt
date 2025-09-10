package com.jumblemint.cows.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jumblemint.cows.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    // Logo scale animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    
    // Text fade animation
    var textVisible by remember { mutableStateOf(false) }
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(1000, easing = EaseInOutCubic),
        label = "textAlpha"
    )
    
    // Trigger animations and navigation
    LaunchedEffect(Unit) {
        delay(500) // Wait before showing text
        textVisible = true
        delay(2500) // Total splash duration
        onSplashFinished()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Icon with animation
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Herd Manager Logo",
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name with fade animation
            Text(
                text = "Herd Manager",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.alpha(textAlpha)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline with fade animation
            Text(
                text = "Manage your cattle with ease",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}