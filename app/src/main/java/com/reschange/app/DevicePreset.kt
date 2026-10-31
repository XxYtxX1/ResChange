package com.reschange.app

data class DevicePreset(
    val name: String,
    val width: Int,   // 0 = custom / unset
    val height: Int,
    val dpi: Int,
    val isCustom: Boolean = false
) {
    val label: String get() = if (isCustom || width == 0) "Custom" else "${width}×${height}"
    val dpiLabel: String get() = if (isCustom || dpi == 0) "—" else "$dpi dpi"
}
