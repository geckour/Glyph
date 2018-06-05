package jp.org.example.geckour.glyph.util

import android.content.SharedPreferences

enum class Key {
    GAME_MODE,
    VIBRATE,
    SHOW_COUNT,
    LEVEL_MIN,
    LEVEL_MAX,
    DONATE
}

enum class HintType(val displayName: String) {
    NAME_AND_GLYPH("NAME + GLYPH"),
    NAME("NAME"),
    GLYPH("GLYPH")
}

fun SharedPreferences.getBooleanValue(key: Key): Boolean =
        contains(key.name)
                && getBoolean(key.name, false)

fun SharedPreferences.getIntValue(key: Key, defaultValue: Int = -1): Int =
        if (contains(key.name))
            getInt(key.name, defaultValue)
        else defaultValue