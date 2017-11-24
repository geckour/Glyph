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
import jp.org.example.geckour.glyph.fragment.CheckAnswerFragment
import jp.org.example.geckour.glyph.fragment.MainFragment
import jp.org.example.geckour.glyph.fragment.model.Result

class MainActivity : AppCompatActivity() {

    enum class Mode {
        NORMAL,
        WEAKNESS
    }

    companion object {
        val tag: String = MainActivity::class.java.simpleName

        fun createIntent(activity: Activity, mode: Mode): Intent =
                Intent(activity, MainActivity::class.java).apply {
                    putExtra(ARGS_MODE, mode)
                }

        private val ARGS_MODE = "mode"

        var hacks: Int = 0
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mode: Mode

    internal var level: Int? = null
    internal var sequenceId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        if (savedInstanceState == null) {
            hacks++

            if (intent.hasExtra(ARGS_MODE)) mode = intent.getSerializableExtra(ARGS_MODE) as Mode

            val fragment = MainFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment, MainFragment.tag)
                    .commit()
        } else {
            mode = savedInstanceState.getSerializable(ARGS_MODE) as Mode
        }

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.putSerializable(ARGS_MODE, mode)
    }

    override fun onBackPressed() {
        finish()
    }

    internal fun transitionForCheckAnswer(results: List<Result>, allowableTime: Long) {
        val fragment = CheckAnswerFragment.newInstance(results, allowableTime)
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, CheckAnswerFragment.tag)
                .addToBackStack(CheckAnswerFragment.tag)
                .commit()
    }

    internal fun getMode(): Mode = mode

    internal fun onRetry() = supportFragmentManager.popBackStack()

    internal fun onNext() = startActivity(MainActivity.createIntent(this, mode))
}