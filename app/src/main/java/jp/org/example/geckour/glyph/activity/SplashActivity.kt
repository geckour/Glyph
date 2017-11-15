package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.sp
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivitySplashBinding
import timber.log.Timber

class SplashActivity : Activity() {
    private lateinit var binding: ActivitySplashBinding
    private val tag: String = this::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        val t: Tracker? = (application as App).getTracker(App.TrackerName.APP_TRACKER)
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())

        if (sp.getString("min_level", null) == null) {
            sp.edit()?.putString("min_level", "0")?.apply()
        }
        if (sp.getString("max_level", null) == null) {
            sp.edit()?.putString("max_level", "8")?.apply()
        }
        if (sp.getInt("countView", -1) != -1) {
            sp.edit()?.putInt("viewCount", 1)?.apply()
        }

        listOf(
                binding.buttonHack,
                binding.buttonOpt,
                binding.buttonDict,
                binding.buttonWeak
        ).forEach {
            it.typeface = ResourcesCompat.getFont(this, R.font.coda_regular)
        }

        binding.buttonHack.setOnClickListener { onClickHack() }
        binding.buttonOpt.setOnClickListener { onClickSetting() }
        binding.buttonDict.setOnClickListener { onClickDictionary() }
        binding.buttonWeak.setOnClickListener { onClickWeakness() }
    }

    override fun onResume() {
        super.onResume()
        sp.edit().putInt("viewCount", 1).apply() // FIXME: 0スタートのが自然
    }



    private fun onClickHack() =
        startActivity(MainActivity.createIntent(this, MainActivity.Mode.NORMAL))

    private fun onClickSetting() =
        startActivityForResult(Pref.createIntent(this), 0)

    private fun onClickDictionary() =
        startActivity(DictActivity.createIntent(this))

    private fun onClickWeakness() =
        startActivity(MainActivity.createIntent(this, MainActivity.Mode.WEAKNESS))

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        //Pref.javaからの戻り値の場合
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                Timber.d("Setting is changed.")
            }
        }
    }
}