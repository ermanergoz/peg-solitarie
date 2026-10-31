package com.app.pegsolitaire.ui.backdrop

// Red-commit stub: the pose functions' API, returning nothing meaningful.

const val SHEEN_HIDDEN = -1f

const val WAVE_REST_OFFSET = 0f

const val LIGHT_REST_DY = 0f

fun waveSwing(wave: WaveModel, timeMs: Long): Float = 0f

fun waveOffsetX(wave: WaveModel, timeMs: Long): Float = 0f

fun waveOffsetY(wave: WaveModel, timeMs: Long): Float = 0f

fun lightPhase(light: LightModel, timeMs: Long): Float = 0f

fun lightDyAt(scene: SceneModel, phase: Float): Float = 0f

fun lightEnvelope(phase: Float): Float = 0f

fun lightCalm(scene: SceneModel, light: LightModel, dy: Float): Float = 0f

fun lightDy(scene: SceneModel, light: LightModel, timeMs: Long): Float = 0f

fun lightAlpha(scene: SceneModel, light: LightModel, timeMs: Long): Float = 0f

fun lightRestAlpha(scene: SceneModel, light: LightModel): Float = 0f

fun gelSheenProgress(timeMs: Long, delayMs: Long): Float = 0f
