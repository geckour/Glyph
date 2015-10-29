package jp.org.example.geckour.glyph

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.util.Log

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker

class Pref : android.preference.PreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sp = PreferenceManager.getDefaultSharedPreferences(this)
        try {
            min = Integer.parseInt(sp?.getString("min_level", "0"))
            max = Integer.parseInt(sp?.getString("max_level", "8"))
        } catch (e: Exception) {
            min = 0
            max = 8
            Log.v("E", "Can't translate level into Integer. min:$min, max:$max")
        }

        fragmentManager.beginTransaction().replace(android.R.id.content, prefFragment()).commit()

        val t: Tracker? = (application as Analytics).getTracker(Analytics.TrackerName.APP_TRACKER)
        t?.setScreenName("PreferenceActivity")
        t?.send(HitBuilders.AppViewBuilder().build())
    }

    class prefFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //設定画面を追加
            addPreferencesFromResource(R.xml.preferences)
            //preferences.xml内のmin_levelが変更されたかをListen
            minLevelPref = findPreference("min_level") as EditTextPreference
            minLevelPref?.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
                    return isMinLevelChanged(preference, newValue)
                }
            }
            minLevelPref?.summary = minLevelPref?.text

            //preferences.xml内のmax_levelが変更されたかをListen
            maxLevelPref = findPreference("max_level") as EditTextPreference
            maxLevelPref?.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
                    return isMaxLevelChanged(preference, newValue)
                }
            }
            maxLevelPref?.summary = maxLevelPref?.text
        }
    }

    companion object {
        internal var sp: SharedPreferences? = null
        internal var min: Int = 0
        internal var max: Int = 0
        internal var minLevelPref: EditTextPreference? = null
        internal var maxLevelPref: EditTextPreference? = null

        fun isMinLevelChanged(preference: Preference, newValue: Any): Boolean {
            val level: Int
            var isException = false
            try {
                level = Integer.parseInt(newValue.toString())
            } catch (e: Exception) {
                isException = true
                Log.v("E", "Can't translate minimum level into Integer. min:$min, max:$max")
                sp?.edit()?.putString("min_level", min.toString())?.apply()
                level = min
            }

            val maxLevel = Integer.parseInt(sp?.getString("max_level", "0"))
            if (-1 < level && level < 9) {
                if (level > maxLevel) {
                    sp?.edit()?.putString("min_level", maxLevel.toString())?.apply()
                    preference.summary = maxLevel.toString()
                    sp?.edit()?.putString("max_level", level.toString())?.apply()
                    maxLevelPref?.summary = level.toString()
                    return false
                } else {
                    if (isException) {
                        preference.summary = min.toString()
                        return false
                    } else {
                        preference.summary = newValue.toString()
                        return true
                    }
                }
            } else {
                if (level > maxLevel) {
                    sp?.edit()?.putString("min_level", maxLevel.toString())?.apply()
                    preference.summary = maxLevel.toString()
                } else {
                    sp?.edit()?.putString("min_level", "0")?.apply()
                    preference.summary = "0"
                }
                return false
            }
        }

        fun isMaxLevelChanged(preference: Preference, newValue: Any): Boolean {
            val level: Int
            var isException = false
            try {
                level = Integer.parseInt(newValue.toString())
            } catch (e: Exception) {
                isException = true
                Log.v("E", "Can't translate maximum level into Integer. min:$min, max:$max")
                sp?.edit()?.putString("max_level", max.toString())?.apply()
                level = max
            }

            val minLevel = Integer.parseInt(sp?.getString("min_level", "0"))
            if (-1 < level && level < 9) {
                if (level < minLevel) {
                    sp?.edit()?.putString("min_level", level.toString())?.apply()
                    minLevelPref?.summary = level.toString()
                    sp?.edit()?.putString("max_level", minLevel.toString())?.apply()
                    preference.summary = minLevel.toString()
                    return false
                } else {
                    if (isException) {
                        preference.summary = max.toString()
                        return false
                    } else {
                        preference.summary = newValue.toString()
                        return true
                    }
                }
            } else {
                if (level < minLevel) {
                    sp?.edit()?.putString("max_level", minLevel.toString())?.apply()
                    preference.summary = minLevel.toString()
                } else {
                    sp?.edit()?.putString("max_level", "8")?.apply()
                    preference.summary = "8"
                }
                return false
            }
        }
    }
}
