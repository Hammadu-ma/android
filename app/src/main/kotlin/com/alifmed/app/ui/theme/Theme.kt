package com.alifmed.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AlifMedColorScheme = lightColorScheme(
    primary = BrandDark,
    onPrimary = BrandBackground,
    background = BrandBackground,
    surface = BrandBackground,
    onBackground = BrandDark,
    onSurface = BrandDark,
)

@Composable
fun AlifMedTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlifMedColorScheme,
        content = content
    )
}
