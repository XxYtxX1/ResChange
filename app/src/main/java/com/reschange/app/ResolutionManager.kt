package com.reschange.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class ResolutionManager(private val context: Context) {

    companion object {
        private const val TAG = "ResolutionManager"
        private const val PREFS_NAME = "zayon_resolution"
        const val KEY_APPLIED     = "applied"
        const val KEY_APPLIED_W   = "applied_w"
        const val KEY_APPLIED_H   = "applied_h"
        const val KEY_APPLIED_DPI = "applied_dpi"
        const val KEY_BASE_W      = "base_w"
        const val KEY_BASE_H      = "base_h"
        const val KEY_BASE_DPI    = "base_dpi"

        // Default fallback values (1080p @ 400dpi as per report)
        private const val DEFAULT_W   = 1080
        private const val DEFAULT_H   = 2400
        private const val DEFAULT_DPI = 400
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    enum class ShellMode { SHIZUKU, ROOT, NONE }

    val shellMode: ShellMode
        get() = when {
            ShizukuBridge.isBound -> ShellMode.SHIZUKU
            isRootAvailable()     -> ShellMode.ROOT
            else                  -> ShellMode.NONE
        }

    private fun isRootAvailable(): Boolean = try {
        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor() == 0 && "uid=0" in out
    } catch (e: Exception) { false }

    /** Execute shell command; returns exit code. */
    fun execShell(cmd: String): Int {
        Log.d(TAG, "exec: $cmd")
        return when {
            ShizukuBridge.isBound -> {
                ShizukuBridge.service?.execute(cmd) ?: -1
            }
            isRootAvailable() -> try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
            } catch (e: Exception) { -1 }
            else -> -1
        }
    }

    /** Read output of shell command. */
    fun readShell(cmd: String): String {
        return when {
            ShizukuBridge.isBound -> {
                ShizukuBridge.service?.read(cmd) ?: ""
            }
            isRootAvailable() -> try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val out = proc.inputStream.bufferedReader().readText().trim()
                proc.waitFor()
                out
            } catch (e: Exception) { "" }
            else -> ""
        }
    }

    /** Reads current display size from wm size output. */
    fun currentSize(): Pair<Int, Int>? {
        val out = readShell("wm size")
        val match = Regex("""(\d+)x(\d+)""").find(out) ?: return null
        return Pair(match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }

    /** Reads current DPI from wm density output. */
    fun currentDpi(): Int {
        val out = readShell("wm density")
        // "Physical density: 400" or "Override density: 240"
        return Regex("""density:\s*(\d+)""").find(out)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /**
     * Saves original resolution to prefs on first apply.
     * This is the baseline we restore to on reset.
     */
    private fun ensureBaseSaved() {
        if (!prefs.contains(KEY_BASE_W)) {
            val (w, h) = currentSize() ?: Pair(DEFAULT_W, DEFAULT_H)
            val dpi = currentDpi().takeIf { it > 0 } ?: DEFAULT_DPI
            prefs.edit()
                .putInt(KEY_BASE_W, w)
                .putInt(KEY_BASE_H, h)
                .putInt(KEY_BASE_DPI, dpi)
                .apply()
            Log.d(TAG, "Saved base: ${w}x${h} @ $dpi")
        }
    }

    /** Apply a preset's resolution. Returns true on success. */
    fun apply(preset: DevicePreset): Boolean {
        if (preset.width <= 0 || preset.height <= 0 || preset.dpi <= 0) return false
        ensureBaseSaved()

        val sizeOk = execShell("wm size ${preset.width}x${preset.height}") == 0
        val dpiOk  = execShell("wm density ${preset.dpi}") == 0

        return if (sizeOk && dpiOk) {
            prefs.edit()
                .putBoolean(KEY_APPLIED, true)
                .putInt(KEY_APPLIED_W, preset.width)
                .putInt(KEY_APPLIED_H, preset.height)
                .putInt(KEY_APPLIED_DPI, preset.dpi)
                .apply()
            Log.d(TAG, "Applied ${preset.width}x${preset.height} @ ${preset.dpi}")
            true
        } else {
            Log.e(TAG, "Apply failed: sizeOk=$sizeOk dpiOk=$dpiOk")
            false
        }
    }

    /** Reset to original resolution. Returns true on success. */
    fun reset(): Boolean {
        val sizeOk = execShell("wm size reset") == 0
        val dpiOk  = execShell("wm density reset") == 0
        return if (sizeOk && dpiOk) {
            prefs.edit().putBoolean(KEY_APPLIED, false).apply()
            Log.d(TAG, "Reset done")
            true
        } else {
            Log.e(TAG, "Reset failed")
            false
        }
    }

    /** Restore saved resolution on boot. */
    fun restoreOnBoot() {
        if (!prefs.getBoolean(KEY_APPLIED, false)) return
        val w   = prefs.getInt(KEY_APPLIED_W, 0)
        val h   = prefs.getInt(KEY_APPLIED_H, 0)
        val dpi = prefs.getInt(KEY_APPLIED_DPI, 0)
        if (w > 0 && h > 0 && dpi > 0) {
            execShell("wm size ${w}x${h}")
            execShell("wm density $dpi")
            Log.d(TAG, "Boot restored: ${w}x${h} @ $dpi")
        }
    }

    fun isApplied(): Boolean = prefs.getBoolean(KEY_APPLIED, false)

    fun appliedPreset(): DevicePreset? {
        if (!isApplied()) return null
        return DevicePreset(
            name   = "Applied",
            width  = prefs.getInt(KEY_APPLIED_W, 0),
            height = prefs.getInt(KEY_APPLIED_H, 0),
            dpi    = prefs.getInt(KEY_APPLIED_DPI, 0)
        )
    }

    fun basePreset(): DevicePreset = DevicePreset(
        name   = "Base",
        width  = prefs.getInt(KEY_BASE_W, DEFAULT_W),
        height = prefs.getInt(KEY_BASE_H, DEFAULT_H),
        dpi    = prefs.getInt(KEY_BASE_DPI, DEFAULT_DPI)
    )
}
