package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityStatisticsBinding
import jp.org.example.geckour.glyph.fragment.adapter.StatsFragmentPagerAdapter

class StatsActivity: AppCompatActivity() {

    companion object {
        private val tag: String = StatsActivity::class.java.simpleName

        fun createIntent(activity: Activity): Intent =
                Intent(activity, StatsActivity::class.java)
    }

    private lateinit var binding: ActivityStatisticsBinding
    internal val bitmap by lazy {
        val size = (40 * resources.displayMetrics.density).toInt()
        Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal), size, size, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            binding = DataBindingUtil.setContentView(this, R.layout.activity_statistics)

            binding.viewPager.adapter = StatsFragmentPagerAdapter(supportFragmentManager)
            binding.tab.setupWithViewPager(binding.viewPager)
        }

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(StatsActivity.tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }
}