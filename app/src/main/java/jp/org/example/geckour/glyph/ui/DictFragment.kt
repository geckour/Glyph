package jp.org.example.geckour.glyph.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.fragment.app.Fragment
import io.realm.Realm
import jp.org.example.geckour.glyph.databinding.FragmentMainBinding
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.util.*
import jp.org.example.geckour.glyph.ui.view.AnimateView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class DictFragment : Fragment() {

    companion object {
        val tag: String = DictFragment::class.java.simpleName

        fun newInstance(): DictFragment = DictFragment()
    }

    private lateinit var binding: FragmentMainBinding
    private lateinit var realm: Realm
    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }

    private val throughDots: ArrayList<Int> = ArrayList()

    private var fromX = -1f
    private var fromY = -1f

    private var doVibrate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding.animateView.setGrainAlphaModeIntoDictionary()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideLeftButton()
        hideRightButton()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        doVibrate = sharedPreferences.getBooleanValue(Key.VIBRATE)
        Timber.d("doVibrate: $doVibrate")

        binding.animateView.setOnTouchListener { _, event ->
            when (binding.animateView.getInputState()) {
                AnimateView.InputState.DISABLED -> false

                AnimateView.InputState.ENABLED -> {
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
                                if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) activity?.vibrate(binding.dotsView)
                            }
                            throughDots.addAll(collision)
                            binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                            binding.animateView.addParticle(event.x, event.y)
                            fromX = event.x
                            fromY = event.y
                            true
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                                if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) activity?.vibrate(binding.dotsView)
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

                AnimateView.InputState.COMMAND -> false
            }
        }
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