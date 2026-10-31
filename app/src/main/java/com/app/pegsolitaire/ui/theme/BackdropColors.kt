package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.app.pegsolitaire.ui.backdrop.LightKind
import com.app.pegsolitaire.ui.backdrop.SkyRole

/**
 * The backdrop's sixteen colours per theme (backdrop spec §4.1), packaged from the
 * `Color.kt` vals the way [ClayColors] packages the clay ones, and provided by `AppTheme` through
 * [LocalBackdropColors]. No call site names a hex.
 *
 * The sky's top is the theme's `background` (BD-09), so the status-bar area of every screen is
 * what it was before the backdrop.
 */
@Immutable
data class BackdropColors(
    val skyTop: Color,
    val skyMid: Color,
    val skyLow: Color,
    val skyDeep: Color,
    val sparseLow: Color,
    val waveCrest: Color,
    val waveBody: Color,
    val waveDeep: Color,
    val waveLip: Color,
    val waveSheen: Color,
    val pearlCore: Color,
    val pearlRim: Color,
    val pinkCore: Color,
    val pinkRim: Color,
    val violetCore: Color,
    val violetRim: Color,
) {
    /** The colour a sky stop of [role] takes. */
    fun sky(role: SkyRole): Color =
        when (role) {
            SkyRole.Top -> skyTop
            SkyRole.Mid -> skyMid
            SkyRole.Low -> skyLow
            SkyRole.Deep -> skyDeep
            SkyRole.SparseLow -> sparseLow
        }

    /** A light of [kind]'s core colour, from its centre to `LIGHT_CORE_STOP`. */
    fun lightCore(kind: LightKind): Color =
        when (kind) {
            LightKind.Pearl -> pearlCore
            LightKind.Pink -> pinkCore
            LightKind.Violet -> violetCore
        }

    /** A light of [kind]'s rim colour, at `LIGHT_RIM_STOP`: its extreme point. */
    fun lightRim(kind: LightKind): Color =
        when (kind) {
            LightKind.Pearl -> pearlRim
            LightKind.Pink -> pinkRim
            LightKind.Violet -> violetRim
        }
}

internal val LightBackdropColors =
    BackdropColors(
        skyTop = LightBackdropSkyTop,
        skyMid = LightBackdropSkyMid,
        skyLow = LightBackdropSkyLow,
        skyDeep = LightBackdropSkyDeep,
        sparseLow = LightBackdropSparseLow,
        waveCrest = LightBackdropWaveCrest,
        waveBody = LightBackdropWaveBody,
        waveDeep = LightBackdropWaveDeep,
        waveLip = LightBackdropWaveLip,
        waveSheen = LightBackdropWaveSheen,
        pearlCore = LightBackdropLightPearlCore,
        pearlRim = LightBackdropLightPearlRim,
        pinkCore = LightBackdropLightPinkCore,
        pinkRim = LightBackdropLightPinkRim,
        violetCore = LightBackdropLightVioletCore,
        violetRim = LightBackdropLightVioletRim,
    )

internal val DarkBackdropColors =
    BackdropColors(
        skyTop = DarkBackdropSkyTop,
        skyMid = DarkBackdropSkyMid,
        skyLow = DarkBackdropSkyLow,
        skyDeep = DarkBackdropSkyDeep,
        sparseLow = DarkBackdropSparseLow,
        waveCrest = DarkBackdropWaveCrest,
        waveBody = DarkBackdropWaveBody,
        waveDeep = DarkBackdropWaveDeep,
        waveLip = DarkBackdropWaveLip,
        waveSheen = DarkBackdropWaveSheen,
        pearlCore = DarkBackdropLightPearlCore,
        pearlRim = DarkBackdropLightPearlRim,
        pinkCore = DarkBackdropLightPinkCore,
        pinkRim = DarkBackdropLightPinkRim,
        violetCore = DarkBackdropLightVioletCore,
        violetRim = DarkBackdropLightVioletRim,
    )

/** The active backdrop palette, provided by `AppTheme` beside [LocalClayColors]. */
val LocalBackdropColors = staticCompositionLocalOf { LightBackdropColors }
