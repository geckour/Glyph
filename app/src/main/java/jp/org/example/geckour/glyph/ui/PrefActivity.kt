package jp.org.example.geckour.glyph.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.org.example.geckour.glyph.util.setCrashlytics

class PrefActivity : AppCompatActivity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, PrefActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCrashlytics()

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, PrefFragment.newInstance(), PrefFragment.tag)
                    .commit()
    }
}