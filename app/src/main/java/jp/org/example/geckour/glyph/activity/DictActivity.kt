package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityMainBinding
import jp.org.example.geckour.glyph.fragment.DictFragment

class DictActivity : AppCompatActivity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, DictActivity::class.java)
    }

    private val tag = this::class.java.simpleName
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val fragment = DictFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .add(R.id.container, fragment, DictFragment.tag)
                .commit()

        val t: Tracker? = (application as App).getTracker(App.TrackerName.APP_TRACKER)
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }
}