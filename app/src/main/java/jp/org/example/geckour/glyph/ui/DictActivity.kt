package jp.org.example.geckour.glyph.ui

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityMainBinding
import jp.org.example.geckour.glyph.util.setCrashlytics

class DictActivity : AppCompatActivity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, DictActivity::class.java)
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (savedInstanceState == null) {
            val fragment = DictFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragment, DictFragment.tag)
                    .commit()
        }
    }
}