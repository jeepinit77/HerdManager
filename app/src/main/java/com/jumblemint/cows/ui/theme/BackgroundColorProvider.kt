package com.jumblemint.cows.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalBackgroundColor = staticCompositionLocalOf { Color.Transparent }

@Composable
fun BackgroundColorProvider(
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBackgroundColor provides backgroundColor,
        content = content
    )
}