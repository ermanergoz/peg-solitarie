package com.app.pegsolitaire.ui.backdrop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Whether battery saver is on (B-22 rule 4, the tech-lead's A5.4): read once, then followed through
 * `ACTION_POWER_SAVE_MODE_CHANGED`. While it is true the backdrop draws its rest pose and runs no
 * clock, exactly as at animator scale 0.
 *
 * Composed only behind the scene-ready gate, so no system service and no receiver is touched on the
 * host's first frame (backdrop spec §7). The receiver is not exported: the system still delivers
 * its own broadcasts to it.
 */
@Composable
internal fun rememberPowerSaveMode(): State<Boolean> {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    val saving = remember(powerManager) { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    DisposableEffect(context, powerManager) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    receiverContext: Context,
                    intent: Intent,
                ) {
                    saving.value = powerManager?.isPowerSaveMode == true
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return saving
}
