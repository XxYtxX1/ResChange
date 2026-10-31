package com.reschange.app

object PresetRepository {
    val presets: List<DevicePreset> = listOf(
        DevicePreset("Custom",             0,    0,    0,   isCustom = true),
        DevicePreset("Pixel Mode 🎮",      480,  854,  120),
        DevicePreset("iPhone 17 Pro Max",  1320, 2868, 460),
        DevicePreset("iPhone 18 Pro Max",  1320, 2868, 460),
        DevicePreset("Galaxy S25 Ultra",   1440, 3120, 500),
        DevicePreset("Galaxy Z Fold7",     1968, 2184, 368),
        DevicePreset("Pixel 10 Pro XL",    1344, 2992, 486),
        DevicePreset("ROG Phone 9 Pro",    1080, 2400, 388),
        DevicePreset("iPad Pro 13\"",      2064, 2752, 264),
    )
}
