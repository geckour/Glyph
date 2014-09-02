package jp.org.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
    }

    public void onClickStart(View v) {
        startActivity(new Intent(SplashActivity.this, MyActivity.class));
    }

    public void onClickSetting(View v) {
        startActivity(new Intent(SplashActivity.this, Pref.class));
    }
}
