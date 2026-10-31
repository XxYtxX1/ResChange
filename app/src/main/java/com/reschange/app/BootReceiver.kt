package com.reschange.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restores the previously applied resolution after device reboot.
 * Requires RECEIVE_BOOT_COMPLETED permission.
 *
 * Note: Root mode works fine here since Shizuku hasn't started yet at boot;
 * if using Shizuku-only, user must open the app manually after reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d("BootReceiver", "Boot completed — restoring resolution")
        val manager = ResolutionManager(context)
        // Root is the only viable shell at early boot (Shizuku not yet started)
        manager.restoreOnBoot()
    }
}
