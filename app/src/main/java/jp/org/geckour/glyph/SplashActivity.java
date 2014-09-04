package jp.org.geckour.glyph;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
        startActivityForResult(new Intent(SplashActivity.this, MyActivity.class), 0);
    }

    public void onClickSetting(View v) {
        startActivity(new Intent(SplashActivity.this, Pref.class));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        //Pref.javaからの戻り値の場合
        if (requestCode == 0){
            if (resultCode == Activity.RESULT_OK) {
                Log.v("echo", "level is changed.");
            }
        }
    }
}
