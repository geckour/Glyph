package jp.org.example.geckour.glyph

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.Button

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker

class SplashActivity : Activity() {
    var sp: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        sp = PreferenceManager.getDefaultSharedPreferences(this)

        val t: Tracker? = (application as Analytics).getTracker(Analytics.TrackerName.APP_TRACKER)
        t?.setScreenName("SplashActivity")
        t?.send(HitBuilders.AppViewBuilder().build())

        if (sp?.getString("min_level", "null") == "null") {
            sp?.edit()?.putString("min_level", "0")?.apply()
        }
        if (sp?.getString("max_level", "null") == "null") {
            sp?.edit()?.putString("max_level", "8")?.apply()
        }
        if (sp?.getInt("countView", -1) != -1) {
            sp?.edit()?.putInt("viewCount", 1)?.apply()
        }

        val actionBar = actionBar
        actionBar?.hide()

        val button1 = findViewById(R.id.button1) as Button
        val button2 = findViewById(R.id.button2) as Button
        val typeface = Typeface.createFromAsset(assets, "Coda-Regular.ttf")
        button1.typeface = typeface
        button2.typeface = typeface
    }

    override fun onResume() {
        super.onResume()
        sp?.edit()?.putInt("viewCount", 1)?.apply()
    }

    fun onClickStart(v: View) {
        startActivity(Intent(this@SplashActivity, MyActivity::class.java))
    }

    fun onClickSetting(v: View) {
        startActivityForResult(Intent(this@SplashActivity, Pref::class.java), 0)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        val tag = "SplashActivity.onActivityResult"
        //Pref.javaからの戻り値の場合
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                Log.v(tag, "Setting is changed.")
            }
        }
    }
}
