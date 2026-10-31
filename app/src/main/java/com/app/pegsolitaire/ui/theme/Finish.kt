package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

// Red-commit stub: the finish's API, drawing nothing.

@Composable
fun Modifier.finishGlow(
    elevation: Dp,
    shape: Shape,
    tint: Color = LocalFinishColors.current.panelGlow,
): Modifier = shadow(elevation = elevation, shape = shape, ambientColor = tint, spotColor = tint)

@Composable
fun Modifier.glossFinish(
    container: FinishedContainer,
    shape: Shape,
    fill: Color? = null,
): Modifier = this.also { check(container.recipe.rim || shape == shape || fill == null) }
