package com.app.pegsolitaire.ui.backdrop

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.app.pegsolitaire.R

/**
 * The backdrop scene's `dimens.xml` tokens (backdrop spec §4.2), resolved once to px so the
 * pure builder ([buildSceneModel]) and the pose functions read no resource and no density.
 *
 * [lightRestPhase] is derived from the two rise tokens, never written as a literal (§4.3): it is
 * the phase at which a light sits at its rest centre.
 */
@Immutable
data class BackdropDimens(
    val lightXs: Float,
    val lightSm: Float,
    val lightMd: Float,
    val lightLg: Float,
    val lightXl: Float,
    val lightXxl: Float,
    val lightRiseBelow: Float,
    val lightRiseAbove: Float,
    val lightCalmFade: Float,
    val waveLipDepth: Float,
    val waveSheenOffset: Float,
    val waveSheenHeight: Float,
    val waveBackBobX: Float,
    val waveBackBobY: Float,
    val waveFrontBobX: Float,
    val waveFrontBobY: Float,
) {
    /** The phase at which a light's rise puts it at its rest centre (`dy` = 0). */
    val lightRestPhase: Float get() = lightRiseBelow / (lightRiseBelow + lightRiseAbove)

    /** The px diameter of a light of [size]. */
    fun diameter(size: LightSize): Float =
        when (size) {
            LightSize.Xs -> lightXs
            LightSize.Sm -> lightSm
            LightSize.Md -> lightMd
            LightSize.Lg -> lightLg
            LightSize.Xl -> lightXl
            LightSize.Xxl -> lightXxl
        }

    companion object {
        /** The tokens as [resources] resolve them, in px. */
        fun from(resources: Resources): BackdropDimens {
            fun px(id: Int): Float = resources.getDimension(id)
            return BackdropDimens(
                lightXs = px(R.dimen.backdrop_light_xs),
                lightSm = px(R.dimen.backdrop_light_sm),
                lightMd = px(R.dimen.backdrop_light_md),
                lightLg = px(R.dimen.backdrop_light_lg),
                lightXl = px(R.dimen.backdrop_light_xl),
                lightXxl = px(R.dimen.backdrop_light_xxl),
                lightRiseBelow = px(R.dimen.backdrop_light_rise_below),
                lightRiseAbove = px(R.dimen.backdrop_light_rise_above),
                lightCalmFade = px(R.dimen.backdrop_light_calm_fade),
                waveLipDepth = px(R.dimen.backdrop_wave_lip_depth),
                waveSheenOffset = px(R.dimen.backdrop_wave_sheen_offset),
                waveSheenHeight = px(R.dimen.backdrop_wave_sheen_height),
                waveBackBobX = px(R.dimen.backdrop_wave_back_bob_x),
                waveBackBobY = px(R.dimen.backdrop_wave_back_bob_y),
                waveFrontBobX = px(R.dimen.backdrop_wave_front_bob_x),
                waveFrontBobY = px(R.dimen.backdrop_wave_front_bob_y),
            )
        }
    }
}

/**
 * The backdrop's px tokens for the current configuration, resolved once per configuration. The
 * configuration is read so a density or a window change re-resolves them.
 */
@Composable
fun rememberBackdropDimens(): BackdropDimens {
    val configuration = LocalConfiguration.current
    val resources = LocalResources.current
    return remember(configuration, resources) { BackdropDimens.from(resources) }
}
