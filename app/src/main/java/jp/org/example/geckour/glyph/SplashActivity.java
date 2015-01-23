package jp.org.example.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class SplashActivity extends Activity {
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Tracker t = ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("SplashActivity");
        t.send(new HitBuilders.AppViewBuilder().build());

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getString("min_level", "null").equals("null")) {
            sp.edit().putString("min_level", "0").apply();
        }
        if (sp.getString("max_level", "null").equals("null")) {
            sp.edit().putString("max_level", "8").apply();
        }
        if (sp.getInt("countView", -1) != -1) {
            sp.edit().putInt("viewCount", 1).apply();
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        Button button1 = (Button)findViewById(R.id.button1);
        Button button2 = (Button)findViewById(R.id.button2);
        Typeface typeface = Typeface.createFromAsset(getAssets(), "Coda-Regular.ttf");
        button1.setTypeface(typeface);
        button2.setTypeface(typeface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sp.edit().putInt("viewCount", 1).apply();
    }

    public void onClickStart(View v) {
        startActivity(new Intent(SplashActivity.this, MyActivity.class));
    }

    public void onClickSetting(View v) {
        startActivityForResult(new Intent(SplashActivity.this, Pref.class), 0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        String tag = "SplashActivity.onActivityResult";
        //Pref.javaからの戻り値の場合
        if (requestCode == 0){
            if (resultCode == Activity.RESULT_OK) {
                Log.v(tag, "Setting is changed.");
            }
        }
    }
}
