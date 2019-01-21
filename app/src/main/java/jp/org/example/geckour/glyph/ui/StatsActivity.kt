package jp.org.example.geckour.glyph.ui

import android.app.Activity
import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityStatisticsBinding
import jp.org.example.geckour.glyph.adapter.StatsFragmentPagerAdapter
import jp.org.example.geckour.glyph.util.setCrashlytics

class StatsActivity : AppCompatActivity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, StatsActivity::class.java)
    }

    private lateinit var binding: ActivityStatisticsBinding
    internal val bitmap by lazy {
        val size = (40 * resources.displayMetrics.density).toInt()

        Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal),
                size, size, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_statistics)

        binding.viewPager.adapter = StatsFragmentPagerAdapter(supportFragmentManager)
        binding.tab.setupWithViewPager(binding.viewPager)
    }
}