package jp.org.example.geckour.glyph.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.coda
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.MainActivity
import jp.org.example.geckour.glyph.databinding.FragmentMainBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper
import jp.org.example.geckour.glyph.fragment.model.Result
import jp.org.example.geckour.glyph.util.*
import jp.org.example.geckour.glyph.view.model.Shaper
import timber.log.Timber
import kotlin.concurrent.thread

class MainFragment: Fragment() {

    companion object {
        val tag: String = this::class.java.simpleName

        fun newInstance(): MainFragment = MainFragment()

        private val STATE_ARGS_LEVEL = "level"
        private val STATE_ARGS_QUESTIONS = "questions"
    }

    private val mainActivity: MainActivity by lazy { activity as MainActivity }

    private lateinit var binding: FragmentMainBinding
    private lateinit var realm: Realm

    private var min = 0
    private var max = 8
    private var level = 0

    private var gameMode: Int = 0
    private var doVibrate: Boolean = false

    private val questions: ArrayList<Shaper> = ArrayList()
    private val throughDots: ArrayList<Int> = ArrayList()
    private val paths: ArrayList<List<Pair<Int, Int>>> = ArrayList()
    private val spentTimes: ArrayList<Long> = ArrayList()

    private var fromX = -1f
    private var fromY = -1f

    private val onLayoutAnimateView: () -> Unit = {
        showSequence {
            // onLayout後に実行しないとwidthが取れないのでaddParticleが呼ばれない
            binding.animateView.apply {
                clearParticle()
                setGrainAlphaModeIntoPrepareInput()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (savedInstanceState == null) {
            try {
                min = Integer.parseInt(App.sp.getString("min_level", "0"))
                Timber.d("min: $min")
            } catch (e: Exception) {
                Timber.e("Can't translate minimum-level to Int.")
            }

            try {
                max = Integer.parseInt(App.sp.getString("max_level", "8"))
                Timber.d("max: $max")
            } catch (e: Exception) {
                Timber.e("Can't translate maximum-level to Int.")
            }

            try {
                gameMode = App.sp.getString("gamemode", "0").toInt()
                Timber.d("gamemode: $gameMode")
            } catch (e: Exception) {
                Timber.e("Can't translate game mode to Int.")
            }

            doVibrate = App.sp.getBoolean("doVibrate", false)
            Timber.d("doVibrate: $doVibrate")

            questions.apply {
                clear()
                addAll(getSequence(mainActivity.sequenceId))
            }
        } else {
            level = savedInstanceState.getInt(STATE_ARGS_LEVEL)
            questions.apply {
                clear()
                addAll(savedInstanceState.getParcelableArrayList(STATE_ARGS_QUESTIONS))
            }
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideLeftButton()
        setRightButton("NEXT") { mainActivity.onNext() }

        binding.animateView.resetInitTime()

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
                        binding.animateView.setGrainAlphaModeIntoInput(Pair(paths.size + 1, getDifficulty(level)))
                        binding.animateView.addParticle(event.x, event.y)
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                            if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) vibrate()
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
                        addCurrentSpentTime()

                        val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                            if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) vibrate()
                        }
                        throughDots.addAll(collision)
                        binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                        paths.add(throughDots.convertDotsListToPaths().getNormalizedPaths())
                        binding.animateView.apply {
                            setGrainAlphaModeIntoFadeout { binding.dotsView.setDotsState { false } }
                            showPaths(paths.last().mapToPointPathsFromDotPaths(binding.dotsView.getDots()))
                        }
                        if (paths.size >= getDifficulty(level)) binding.animateView.setGrainAlphaModeIntoPrepareAnswer()
                        true
                    }
                    else -> true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        paths.clear()
        spentTimes.clear()

        thread {
            while (true) {
                if (binding.animateView.width > 0) {
                    onLayoutAnimateView()
                    return@thread
                }
                Thread.sleep(10)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.apply {
            putInt(STATE_ARGS_LEVEL, level)
            putParcelableArrayList(STATE_ARGS_QUESTIONS, ArrayList(questions))
        }
    }

    private fun getDifficulty(level: Int): Int =
            when (level) {
                in 0..1 -> 1
                2 -> 2
                in 3..5 -> 3
                in 6..7 -> 4
                8 -> 5
                else -> 0
            }

    private fun getAllowableTime(level: Int): Long =
            when (level) {
                in 0..2 -> 20000L
                in 3..8 -> 20000L - 1000L * (level - 2)
                else -> 0L
            }

    private fun setRightButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonRight.apply {
            text = buttonText
            setOnClickListener { predicate(it) }
            visibility = View.VISIBLE
            typeface = coda
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
            typeface = coda
        }
    }

    private fun hideLeftButton() {
        binding.buttonLeft.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }

    private fun showSequence(questions: List<Shaper> = this.questions, onComplete: () -> Unit = {}) {
        if (questions.isNotEmpty()) {
            val difficulty = getDifficulty(level)
            binding.animateView
                    .setGrainAlphaModeIntoQuestion(
                            Pair(difficulty + 1 - questions.size, difficulty),
                            getAllowableTime(level),
                            onStartNextQ = { showSequence(questions.drop(1), onComplete) },
                            onStartInput = { setRightButton("BYPASS") { binding.animateView.setGrainAlphaModeIntoPrepareAnswer() } },
                            onPrepareAnswer = { binding.dotsView.visibility = View.INVISIBLE },
                            onTransitionToCheckAnswer = { checkAnswer() }
                    )
            showShaper(questions.first())
        } else onComplete()
    }

    private fun showShaper(shaper: Shaper) =
        binding.animateView.apply {
            if (gameMode != 2) setShaperName(listOf(shaper.name))
            if (gameMode != 1) {
                showPaths(
                        shaper.dots.convertDotsListToPaths().getNormalizedPaths().mapToPointPathsFromDotPaths(binding.dotsView.getDots())
                ).apply { Timber.d("showing shaper id: ${shaper.id}, name: ${shaper.name}, dots: ${shaper.dots}") }
            }
        }

    private fun getSequence(id: Long? = null): List<Shaper> {
        fun getLevel(): Int = ((max - min + 1) * Math.random() + min).toInt()

        level = mainActivity.level ?: getLevel().apply { Timber.d("level: $this") }
        val difficulty = getDifficulty(level).apply { Timber.d("difficulty: $this") }
        mainActivity.level = level
        return when (difficulty) {
            1 -> {
                realm.where(DBShaper::class.java).count().let {
                    realm.where(DBShaper::class.java)
                            .equalTo("id", id ?: (Math.random() * it).toLong().apply { mainActivity.sequenceId = this })
                            .findFirst()?.let { listOf(it.parse()) } ?: listOf()
                }
            }

            in 2..5 -> {
                realm.where(Sequence::class.java)
                        .equalTo("size", difficulty)
                        .let { sequences ->
                            val first = sequences.findFirst()?.id
                            val last = sequences.findAll().lastOrNull()?.id

                            if (first == null || last == null) return listOf()
                            else sequences.equalTo("id", id ?: (Math.random() * (last - first) + first).toLong().apply { mainActivity.sequenceId = this })
                                    .findAll().firstOrNull()?.message?.toList()
                        }?.map { it.parse() } ?: listOf()
            }

            else -> listOf()
        }
    }

    private fun addCurrentSpentTime() = spentTimes.add(System.currentTimeMillis() - binding.animateView.getInputStartTime() - spentTimes.sum())

    private fun checkAnswer() =
            mainActivity.transitionForheckAnswer(
                    questions.mapIndexed { i, q ->
                        if (i < paths.size) Result(q.id, q.match(paths[i]), spentTimes[i])
                        else Result(q.id, false, 0L)
                    },
                    getAllowableTime(level)
            )
}