package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.SharedPreferences
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.preference.PreferenceManager

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.coda
import jp.org.example.geckour.glyph.App.Companion.scale
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.MainActivity.Companion.hacks
import jp.org.example.geckour.glyph.databinding.ActivitySplashBinding

class SplashActivity : Activity() {

    companion object {
        private val tag: String = SplashActivity::class.java.simpleName
    }

    private lateinit var binding: ActivitySplashBinding
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        if (binding.root.height > 0) scale = binding.root.height.toFloat() / 1280

        if (sp.contains(PrefActivity.Key.SHOW_COUNT.name) && sp.getBoolean(PrefActivity.Key.SHOW_COUNT.name, false)) {
            sp.edit()?.putInt("viewCount", 1)?.apply()
        }

        listOf(
                binding.buttonHack,
                binding.buttonOpt,
                binding.buttonDict,
                binding.buttonWeak
        ).forEach {
            it.typeface = coda
        }

        binding.buttonHack.setOnClickListener { onClickHack() }
        binding.buttonOpt.setOnClickListener { onClickSetting() }
        binding.buttonDict.setOnClickListener { onClickDictionary() }
        binding.buttonWeak.setOnClickListener { onClickWeakness() }

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onResume() {
        super.onResume()
        hacks = 0
    }

    private fun onClickHack() =
        startActivity(MainActivity.createIntent(this, MainActivity.Mode.NORMAL))

    private fun onClickSetting() =
        startActivity(PrefActivity.createIntent(this))

    private fun onClickDictionary() =
        startActivity(DictActivity.createIntent(this))

    private fun onClickWeakness() =
        startActivity(MainActivity.createIntent(this, MainActivity.Mode.WEAKNESS))
}