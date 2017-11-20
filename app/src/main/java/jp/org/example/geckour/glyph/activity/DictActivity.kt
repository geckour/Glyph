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
        private val tag = DictActivity::class.java.simpleName

        fun createIntent(activity: Activity): Intent =
                Intent(activity, DictActivity::class.java)
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

            val fragment = DictFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragment, DictFragment.tag)
                    .commit()
        }

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }
}