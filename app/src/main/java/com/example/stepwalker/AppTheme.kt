package com.example.stepwalker

import androidx.compose.ui.graphics.Color

enum class AppTheme(
    val displayName: String,
    val emoji: String,
    val bgTop: Color,
    val bgBottom: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBg: Color
) {
    MIST(
        "Утренний туман", "🌫",
        Color(0xFFE8EDF2), Color(0xFFD6DEE8),
        Color(0xFF5B7C99), Color(0xFF1F2A37),
        Color(0xFF6B7A8C), Color(0xCCFFFFFF)
    ),
    SUNSET(
        "Закат", "🌅",
        Color(0xFFFFB88C), Color(0xFFE86A6A),
        Color(0xFFFFF1E6), Color(0xFF3A1A14),
        Color(0xFF7A3B33), Color(0x33FFFFFF)
    ),
    MIDNIGHT(
        "Полночь", "🌙",
        Color(0xFF0B1220), Color(0xFF1A2740),
        Color(0xFF7FB2FF), Color(0xFFF0F4FF),
        Color(0xFF8CA0C2), Color(0x22FFFFFF)
    ),
    FOREST(
        "Лес", "🌲",
        Color(0xFF1B3B2F), Color(0xFF0E1F18),
        Color(0xFF8DE0A5), Color(0xFFEAF6EE),
        Color(0xFF8FB39C), Color(0x22FFFFFF)
    ),
    OCEAN(
        "Океан", "🌊",
        Color(0xFF0F3D5C), Color(0xFF071E2C),
        Color(0xFF6FD3E8), Color(0xFFEAF7FB),
        Color(0xFF84A9BD), Color(0x22FFFFFF)
    ),
    LAVENDER(
        "Лаванда", "💜",
        Color(0xFFE8DEF8), Color(0xFFC9B8E8),
        Color(0xFF6D4CA8), Color(0xFF241A38),
        Color(0xFF6B5C8A), Color(0xCCFFFFFF)
    )
}
