package jp.org.example.geckour.glyph;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class Pref extends android.preference.PreferenceActivity {
    static SharedPreferences sp;
    static int min, max;
    static EditTextPreference minLevelPref;
    static EditTextPreference maxLevelPref;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            min = Integer.parseInt(sp.getString("min_level", "0"));
            max = Integer.parseInt(sp.getString("max_level", "8"));
        } catch (Exception e) {
            min = 0;
            max = 8;
            Log.v("E", "Can't translate level into Integer. min:" + min + ", max:" + max);
        }
        getFragmentManager().beginTransaction().replace(android.R.id.content, new prefFragment()).commit();

        Tracker t = ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("PreferenceActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    public static boolean isMinLevelChanged (Preference preference, Object newValue) {
        int level;
        boolean isException = false;
        try {
            level = Integer.parseInt(newValue.toString());
        } catch (Exception e) {
            isException = true;
            Log.v("E", "Can't translate minimum level into Integer. min:" + min + ", max:" + max);
            sp.edit().putString("min_level", String.valueOf(min)).apply();
            level = min;
        }
        int maxLevel = Integer.parseInt(sp.getString("max_level", "0"));
        if (-1 < level && level < 9) {
            if (level > maxLevel) {
                sp.edit().putString("min_level", String.valueOf(maxLevel)).apply();
                preference.setSummary(String.valueOf(maxLevel));
                sp.edit().putString("max_level", String.valueOf(level)).apply();
                maxLevelPref.setSummary(String.valueOf(level));
                return false;
            } else {
                if (isException) {
                    preference.setSummary(String.valueOf(min));
                    return false;
                } else {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            }
        } else {
            if (level > maxLevel) {
                sp.edit().putString("min_level", String.valueOf(maxLevel)).apply();
                preference.setSummary(String.valueOf(maxLevel));
            } else {
                sp.edit().putString("min_level", "0").apply();
                preference.setSummary("0");
            }
            return false;
        }
    }

    public static boolean isMaxLevelChanged (Preference preference, Object newValue) {
        int level;
        boolean isException = false;
        try {
            level = Integer.parseInt(newValue.toString());
        } catch (Exception e) {
            isException = true;
            Log.v("E", "Can't translate maximum level into Integer. min:" + min + ", max:" + max);
            sp.edit().putString("max_level", String.valueOf(max)).apply();
            level = max;
        }
        int minLevel = Integer.parseInt(sp.getString("min_level", "0"));
        if (-1 < level && level < 9) {
            if (level < minLevel) {
                sp.edit().putString("min_level", String.valueOf(level)).apply();
                minLevelPref.setSummary(String.valueOf(level));
                sp.edit().putString("max_level", String.valueOf(minLevel)).apply();
                preference.setSummary(String.valueOf(minLevel));
                return false;
            } else {
                if (isException) {
                    preference.setSummary(String.valueOf(max));
                    return false;
                } else {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            }
        } else {
            if (level < minLevel) {
                sp.edit().putString("max_level", String.valueOf(minLevel)).apply();
                preference.setSummary(String.valueOf(minLevel));
            } else {
                sp.edit().putString("max_level", "8").apply();
                preference.setSummary("8");
            }
            return false;
        }
    }

    public static class prefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //設定画面を追加
            addPreferencesFromResource(R.xml.preferences);
            //preferences.xml内のmin_vertexが変更されたかをListen
            final Preference.OnPreferenceChangeListener MinLevelChangeListener = new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return isMinLevelChanged(preference, newValue);
                }
            };
            //上記リスナーを登録
            EditTextPreference editTextMinLevel = (EditTextPreference) findPreference("min_level");
            editTextMinLevel.setOnPreferenceChangeListener(MinLevelChangeListener);
            editTextMinLevel.setSummary(editTextMinLevel.getText());
            minLevelPref = editTextMinLevel;

            //preferences.xml内のmax_vertexが変更されたかをListen
            final Preference.OnPreferenceChangeListener MaxLevelChangeListener = new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    return isMaxLevelChanged(preference, newValue);
                }
            };
            //上記リスナーを登録
            EditTextPreference editTextMaxLevel = (EditTextPreference) findPreference("max_level");
            editTextMaxLevel.setOnPreferenceChangeListener(MaxLevelChangeListener);
            editTextMaxLevel.setSummary(editTextMaxLevel.getText());
            maxLevelPref = editTextMaxLevel;
        }
    }
}
