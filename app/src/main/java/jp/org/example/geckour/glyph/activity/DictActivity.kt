package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.view.MotionEvent
import android.view.View

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import io.realm.Realm
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityMainBinding
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.util.*

class DictActivity : Activity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, DictActivity::class.java)
    }

    private val tag = this::class.java.simpleName
    private lateinit var binding: ActivityMainBinding
    private lateinit var realm: Realm

    private val throughDots: ArrayList<Int> = ArrayList()

    private var fromX = -1f
    private var fromY = -1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        realm = Realm.getDefaultInstance()

        hideLeftButton()
        hideRightButton()

        binding.animateView.setGrainAlphaModeIntoDictionary()
        binding.animateView.setOnTouchListener { _, event ->
            val lim = 4 * binding.dotsView.scale

            if (!binding.animateView.isInputEnabled()) false
            else {
                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        fromX = event.x
                        fromY = event.y
                        binding.animateView.clearParticle()
                        throughDots.clear()
                        binding.dotsView.setDotsState { false }
                        binding.animateView.resetShaperName()
                        binding.animateView.addParticle(event.x, event.y)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                            if (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0) vibrate()
                        }
                        throughDots.addAll(collision)
                        binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                        if (event.x + lim < fromX || fromX + lim < event.x || event.y + lim < fromY || fromY + lim < event.y) {
                            binding.animateView.addParticle(event.x, event.y)
                        }
                        fromX = event.x
                        fromY = event.y
                        true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                            if (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0) vibrate()
                        }
                        throughDots.addAll(collision)
                        binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                        val path = throughDots.convertDotsListToPaths().getNormalizedPaths()
                        binding.animateView.setShaperName(getShapers(path).map { it.name })
                        binding.animateView.apply { showPaths(path.mapToPointPathsFromDotPaths(binding.dotsView.getDots())) }
                        true
                    }
                    else -> true
                }
            }
        }

        val t: Tracker? = (application as App).getTracker(App.TrackerName.APP_TRACKER)
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    private fun setRightButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonRight.apply {
            text = buttonText
            setOnClickListener { predicate(it) }
            visibility = View.VISIBLE
        }
    }

    private fun hideRightButton() {
        binding.buttonRight.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }

    private fun setLeftButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonLeft.apply {
            text = buttonText
            setOnClickListener { predicate(it) }
            visibility = View.VISIBLE
        }
    }

    private fun hideLeftButton() {
        binding.buttonLeft.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }

    private fun getShapers(path: List<Pair<Int, Int>>): List<Shaper> =
            realm.where(Shaper::class.java).findAll().toList()
                    .filter { it.match(path) }
}