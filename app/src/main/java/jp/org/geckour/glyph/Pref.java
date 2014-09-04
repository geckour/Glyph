package jp.org.geckour.glyph;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Pref extends android.preference.PreferenceActivity {
    static SharedPreferences sp;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new prefFragment()).commit();
    }

    public static boolean isMinLevelChanged (Preference preference, Object newValue) {
        int level = Integer.parseInt(newValue.toString());
        int maxLevel = Integer.parseInt(sp.getString("max_level", "0"));
        if (-1 < level || level < 9) {
            if (level > maxLevel){
                sp.edit().putString("min_level", String.valueOf(maxLevel)).apply();
                sp.edit().putString("max_level", String.valueOf(level)).apply();

                return false;
            } else {
                preference.setSummary(newValue.toString());
                /*
                Intent intent = new Intent();
                intent.putExtra("min_level", "" + newValue);
                setResult(Activity.RESULT_OK, intent);
                */
                return true;
            }
        } else {
            return false;
        }
    }

    public static boolean isMaxLevelChanged (Preference preference, Object newValue) {
        int level = Integer.parseInt(newValue.toString());
        int minLevel = Integer.parseInt(sp.getString("min_level", "0"));
        if (-1 < level || level < 9) {
            if (level < minLevel) {
                sp.edit().putString("min_level", String.valueOf(level)).apply();
                sp.edit().putString("max_level", String.valueOf(minLevel)).apply();

                return false;
            } else {
                preference.setSummary(newValue.toString());
                /*
                Intent intent = new Intent();
                intent.putExtra("max_level", "" + newValue);
                setResult(Activity.RESULT_OK, intent);
                */
                return true;
            }
        } else {
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
        }
    }
}
