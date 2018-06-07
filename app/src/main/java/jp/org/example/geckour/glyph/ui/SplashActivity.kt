package jp.org.example.geckour.glyph.ui

import android.app.Activity
import android.content.SharedPreferences
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.preference.PreferenceManager
import jp.org.example.geckour.glyph.App.Companion.scale
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivitySplashBinding
import jp.org.example.geckour.glyph.ui.MainActivity.Companion.hacks
import jp.org.example.geckour.glyph.util.Key
import jp.org.example.geckour.glyph.util.getBooleanValue
import jp.org.example.geckour.glyph.util.setCrashlytics

class SplashActivity : Activity() {

    private lateinit var binding: ActivitySplashBinding
    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        binding.root.apply {
            viewTreeObserver.addOnGlobalLayoutListener {
                if (width > 0) scale = width.toFloat() / 1000
            }
        }

        if (sharedPreferences.getBooleanValue(Key.SHOW_COUNT)) {
            sharedPreferences.edit()?.putInt("viewCount", 1)?.apply()
        }

        binding.buttonHack.setOnClickListener { onClickHack() }
        binding.buttonWeak.setOnClickListener { onClickWeakness() }
        binding.buttonStats.setOnClickListener { onClickStatistics() }
        binding.buttonDict.setOnClickListener { onClickDictionary() }
        binding.buttonOpt.setOnClickListener { onClickSetting() }
    }

    override fun onResume() {
        super.onResume()
        hacks = 0
    }

    private fun onClickHack() =
            startActivity(MainActivity.createIntent(this, MainActivity.Mode.NORMAL))

    private fun onClickWeakness() =
            startActivity(MainActivity.createIntent(this, MainActivity.Mode.WEAKNESS))

    private fun onClickStatistics() =
            startActivity(StatsActivity.createIntent(this))

    private fun onClickDictionary() =
            startActivity(DictActivity.createIntent(this))

    private fun onClickSetting() =
            startActivity(PrefActivity.createIntent(this))
}