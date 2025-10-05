package com.jumblemint.cows.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Composable that automatically applies contrasting text colors based on background.
 * Use this instead of regular Text when you want automatic luminance-based text color.
 */
@Composable
fun SmartText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    backgroundColor: Color? = null
) {
    val finalColor = when {
        color != Color.Unspecified -> color
        backgroundColor != null -> getContrastingTextColor(backgroundColor)
        else -> style.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = text,
        modifier = modifier,
        color = finalColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun SmartText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    backgroundColor: Color? = null
) {
    val finalColor = when {
        color != Color.Unspecified -> color
        backgroundColor != null -> getContrastingTextColor(backgroundColor)
        else -> style.color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = text,
        modifier = modifier,
        color = finalColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Helper function to get appropriate text color for any background color.
 * Returns white for dark backgrounds, dark for light backgrounds.
 */
fun getTextColorForBackground(backgroundColor: Color): Color {
    return getContrastingTextColor(backgroundColor)
}

// Composition local to track current background color
val LocalBackgroundColor = staticCompositionLocalOf { Color.Unspecified }



/**
 * Text that automatically uses contrasting colors based on background
 */
@Composable
fun AutoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val currentBg = LocalBackgroundColor.current
    val finalColor = when {
        color != Color.Unspecified -> color
        currentBg != Color.Unspecified -> getContrastingTextColor(currentBg)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = text,
        modifier = modifier,
        color = finalColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/**
 * Wrapper that provides background color context to child Text composables
 */
@Composable
fun BackgroundColorProvider(
    backgroundColor: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBackgroundColor provides backgroundColor
    ) {
        content()
    }
}

