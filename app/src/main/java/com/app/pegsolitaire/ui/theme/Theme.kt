package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Minimal Material 3 theme wrapper. Real color/typography/dimens theming is a
 * Slice-2 polish concern (POLI-01); Plan 01 only needs an M3 surface that
 * compiles without any raw `dp`/`sp` literal (enforced by the dp/sp gate).
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
