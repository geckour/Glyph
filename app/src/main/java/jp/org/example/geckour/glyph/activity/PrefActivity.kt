package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.fragment.PrefFragment

class PrefActivity : AppCompatActivity() {

    enum class Key {
        GAME_MODE,
        VIBRATE,
        SHOW_COUNT,
        LEVEL_MIN,
        LEVEL_MAX,
        DONATE
    }

    enum class HintType(val displayName: String) {
        NAME_AND_GLYPH("NAME + GLYPH"),
        NAME("NAME"),
        GLYPH("GLYPH")
    }

    enum class LevelType {
        MIN,
        MAX
    }

    companion object {
        private val tag: String = PrefActivity::class.java.simpleName
        fun createIntent(activity: Activity): Intent =
                Intent(activity, PrefActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction().replace(android.R.id.content, PrefFragment.newInstance(), PrefFragment.tag).commit()

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }
}