package com.jumblemint.cows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.*
import com.jumblemint.cows.ui.theme.AutoText
import com.jumblemint.cows.ui.theme.BackgroundColorProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    title: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    val colorPickerController = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected Color:", style = MaterialTheme.typography.bodyLarge)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
                
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(vertical = 8.dp),
                    controller = colorPickerController,
                    initialColor = currentColor,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        selectedColor = colorEnvelope.color
                    }
                )
                
                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth().height(35.dp),
                    controller = colorPickerController,
                    borderRadius = 8.dp,
                    wheelRadius = 10.dp,
                    wheelColor = Color.White,
                    wheelPaint = remember { androidx.compose.ui.graphics.Paint().apply { color = Color.Black } },
                    initialColor = currentColor
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(selectedColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}