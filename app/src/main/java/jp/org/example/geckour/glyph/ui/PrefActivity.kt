package jp.org.example.geckour.glyph.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class PrefActivity : AppCompatActivity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, PrefActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, PrefFragment.newInstance(), PrefFragment.tag)
                    .commit()
    }
}