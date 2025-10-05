# Automatic Text Color System

This app now includes an automatic luminance-based text color system that ensures text is always readable regardless of background color.

## How It Works

The system uses the luminance value of background colors to automatically determine whether to use light or dark text:
- **Light backgrounds** (luminance > 0.5) → Dark text
- **Dark backgrounds** (luminance ≤ 0.5) → Light text

## Usage Options

### 1. SmartText Composable (Recommended)
Use `SmartText` instead of regular `Text` when you want automatic color handling:

```kotlin
// Automatically determines text color based on background
SmartText(
    text = "Hello World",
    backgroundColor = customBackgroundColor
)

// Falls back to theme colors if no background specified
SmartText(text = "Hello World")

// Explicit color overrides automatic behavior
SmartText(
    text = "Hello World", 
    color = Color.Red,
    backgroundColor = customBackgroundColor
)
```

### 2. Utility Functions
For manual color calculation:

```kotlin
// Get contrasting text color for any background
val textColor = getContrastingTextColor(backgroundColor)

// Extension function
val textColor = backgroundColor.contrastingTextColor()

// Helper function (same as getContrastingTextColor)
val textColor = getTextColorForBackground(backgroundColor)
```

### 3. Regular Text with Manual Colors
For existing code, you can still use regular Text with the utility functions:

```kotlin
Text(
    text = "Hello World",
    color = getContrastingTextColor(backgroundColor)
)
```

## Migration Guide

### Existing Code Patterns
Replace manual luminance calculations:

```kotlin
// OLD - Manual calculation
val textColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

// NEW - Use utility function
val textColor = getContrastingTextColor(backgroundColor)
```

### Component Updates
For components with dynamic backgrounds:

```kotlin
// OLD
Text(
    text = "Sample",
    color = if (bgColor.luminance() < 0.5f) Color.White else Color.Black
)

// NEW - Option 1: SmartText
SmartText(
    text = "Sample",
    backgroundColor = bgColor
)

// NEW - Option 2: Utility function
Text(
    text = "Sample",
    color = getContrastingTextColor(bgColor)
)
```

## Benefits

1. **Consistency**: All text uses the same luminance calculation
2. **Maintainability**: Centralized color logic
3. **Accessibility**: Ensures proper contrast ratios
4. **Theme Support**: Works with all custom themes
5. **Flexibility**: Multiple usage patterns for different needs

## Files Modified

- `ThemeManager.kt`: Added utility functions
- `TextColorUtils.kt`: New file with SmartText composables
- `CowCard.kt`: Updated to use new utilities
- `Theme.kt`: Enhanced theme system

## Best Practices

1. Use `SmartText` for new components with dynamic backgrounds
2. Use utility functions when you need the color value for other purposes
3. Keep explicit colors for branded elements (primary, secondary, etc.)
4. Test with different themes to ensure proper contrast