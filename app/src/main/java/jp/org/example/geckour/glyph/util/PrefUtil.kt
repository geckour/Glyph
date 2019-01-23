package jp.org.example.geckour.glyph.util

import android.content.SharedPreferences

enum class Key(val defaultValue: Any) {
    GAME_MODE(0),
    VIBRATE(true),
    HAPTIC_FEEDBACK(false),
    SHOW_COUNT(false),
    LEVEL_MIN(0),
    LEVEL_MAX(8),
    DONATE(false)
}

enum class HintType(val displayName: String) {
    NAME_AND_GLYPH("NAME + GLYPH"),
    NAME("NAME"),
    GLYPH("GLYPH")
}

fun SharedPreferences.getBooleanValue(key: Key): Boolean =
        if (contains(key.name)) getBoolean(key.name, key.defaultValue as Boolean)
        else key.defaultValue as Boolean

fun SharedPreferences.getIntValue(key: Key): Int =
        if (contains(key.name))
            getInt(key.name, key.defaultValue as Int)
        else key.defaultValue as Int