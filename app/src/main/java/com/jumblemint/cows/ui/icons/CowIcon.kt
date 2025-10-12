package com.jumblemint.cows.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Filled.Cow: ImageVector
    get() {
        if (_cow != null) return _cow!!
        _cow = Builder(
            name = "Filled.Cow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head silhouette
            path(
                fill = SolidColor(Color.Black), // Will be tinted by Icon()
            ) {
                moveTo(12f, 4f)
                curveTo(9f, 4f, 7f, 5.2f, 6.2f, 7f)
                curveTo(4.2f, 7.6f, 2.8f, 9f, 2.8f, 11f)
                curveTo(2.8f, 13.4f, 4.6f, 15.2f, 7f, 16f)
                curveTo(7.5f, 18.5f, 9.5f, 20.3f, 12f, 20.3f)
                curveTo(14.5f, 20.3f, 16.5f, 18.5f, 17f, 16f)
                curveTo(19.4f, 15.2f, 21.2f, 13.4f, 21.2f, 11f)
                curveTo(21.2f, 9f, 19.8f, 7.6f, 17.8f, 7f)
                curveTo(17f, 5.2f, 15f, 4f, 12f, 4f)
                close()

                // Left ear
                moveTo(2.5f, 9f)
                curveTo(4.3f, 7.6f, 6.2f, 7f, 7.5f, 7.1f)
                curveTo(7.4f, 8.2f, 7.4f, 8.9f, 7.7f, 9.9f)
                curveTo(6.3f, 10.2f, 4.5f, 10.8f, 3f, 12f)
                close()

                // Right ear
                moveTo(21.5f, 9f)
                curveTo(19.7f, 7.6f, 17.8f, 7f, 16.5f, 7.1f)
                curveTo(16.6f, 8.2f, 16.6f, 8.9f, 16.3f, 9.9f)
                curveTo(17.7f, 10.2f, 19.5f, 10.8f, 21f, 12f)
                close()

                // Horns
                moveTo(8.8f, 6f)
                curveTo(8.6f, 4.8f, 9.1f, 4f, 10.1f, 3.4f)
                curveTo(10.9f, 2.9f, 11.9f, 2.9f, 12.3f, 2.9f)
                curveTo(11.5f, 3.5f, 11.1f, 4.5f, 11.1f, 5.5f)
                close()

                moveTo(15.2f, 6f)
                curveTo(15.4f, 4.8f, 14.9f, 4f, 13.9f, 3.4f)
                curveTo(13.1f, 2.9f, 12.1f, 2.9f, 11.7f, 2.9f)
                curveTo(12.5f, 3.5f, 12.9f, 4.5f, 12.9f, 5.5f)
                close()

                // Eyes
                moveTo(9.2f, 10.5f)
                curveTo(9.2f, 9.7f, 9.9f, 9f, 10.7f, 9f)
                curveTo(11.5f, 9f, 12.2f, 9.7f, 12.2f, 10.5f)
                curveTo(12.2f, 11.3f, 11.5f, 12f, 10.7f, 12f)
                curveTo(9.9f, 12f, 9.2f, 11.3f, 9.2f, 10.5f)
                close()

                moveTo(12.8f, 10.5f)
                curveTo(12.8f, 9.7f, 13.5f, 9f, 14.3f, 9f)
                curveTo(15.1f, 9f, 15.8f, 9.7f, 15.8f, 10.5f)
                curveTo(15.8f, 11.3f, 15.1f, 12f, 14.3f, 12f)
                curveTo(13.5f, 12f, 12.8f, 11.3f, 12.8f, 10.5f)
                close()

                // Muzzle
                moveTo(8.5f, 13f)
                curveTo(8.5f, 14.7f, 10f, 15.8f, 12f, 15.8f)
                curveTo(14f, 15.8f, 15.5f, 14.7f, 15.5f, 13f)
                curveTo(15.5f, 12.5f, 15f, 12f, 14.4f, 12f)
                lineTo(9.6f, 12f)
                curveTo(9f, 12f, 8.5f, 12.5f, 8.5f, 13f)
                close()

                // Nostrils
                moveTo(10.7f, 13.5f)
                lineTo(10.7f, 14.1f)
                moveTo(13.3f, 13.5f)
                lineTo(13.3f, 14.1f)
            }
        }.build()
        return _cow!!
    }

private var _cow: ImageVector? = null
