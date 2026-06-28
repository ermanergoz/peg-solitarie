package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.Typography

/**
 * Material 3 default type scale.
 *
 * Phase 1 uses the stock M3 `Typography()` on the platform default typeface — the
 * scale already spans exactly the two weights the contract allows (Regular 400 +
 * Medium 500) and maps every role to sp internally, so composables reference
 * `MaterialTheme.typography.*` and never a raw `sp` literal. Custom fonts are a
 * Slice-2 polish concern (POLI-01).
 */
internal val AppTypography = Typography()
