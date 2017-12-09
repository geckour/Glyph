package jp.org.example.geckour.glyph.fragment

import android.content.SharedPreferences
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import io.realm.Realm
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.coda
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.MainActivity
import jp.org.example.geckour.glyph.activity.PrefActivity
import jp.org.example.geckour.glyph.databinding.FragmentMainBinding
import jp.org.example.geckour.glyph.db.DBInitialData
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper
import jp.org.example.geckour.glyph.fragment.model.Result
import jp.org.example.geckour.glyph.util.*
import jp.org.example.geckour.glyph.view.AnimateView
import jp.org.example.geckour.glyph.view.model.Shaper
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import timber.log.Timber

class MainFragment: Fragment() {

    companion object {
        val tag: String = MainFragment::class.java.simpleName

        fun newInstance(): MainFragment = MainFragment()

        private val STATE_ARGS_LEVEL = "level"
        private val STATE_ARGS_QUESTIONS = "questions"
    }

    private val mainActivity: MainActivity by lazy { activity as MainActivity }

    private lateinit var binding: FragmentMainBinding
    private lateinit var realm: Realm
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }

    private var min = 0
    private var max = 8
    private var level = 0

    private var gameMode: Int = 0
    private var doVibrate: Boolean = true

    private val commandMaster: List<Shaper> by lazy {
        realm.where(DBShaper::class.java)
                .equalTo("name", DBInitialData.Shaper.COMPLEX.displayName)
                .or().equalTo("name", DBInitialData.Shaper.SIMPLE.displayName)
                .or().equalTo("name", DBInitialData.Shaper.MORE.displayName)
                .or().equalTo("name", DBInitialData.Shaper.LESS.displayName)
                .findAll()
                .toList()
                .map { it.parse() }
    }

    private val questions: ArrayList<Shaper> = ArrayList()
    private val throughDots: ArrayList<Int> = ArrayList()
    private val paths: MutableList<List<Pair<Int, Int>>> = ArrayList()
    private val spentTimes: ArrayList<Long> = ArrayList()

    private var fromX = -1f
    private var fromY = -1f

    private val onTimeUpForCommand: () -> Unit = {
        if (getTouchStatus() == MotionEvent.ACTION_UP) {
            clearFlash()
            hideDialog()
            binding.animateView.clearParticle()
            binding.dotsView.setDotsState { false }
            showSequence {
                binding.animateView.apply {
                    clearParticle()
                    setGrainAlphaModeIntoPrepareInput()
                }
                showFlashForNoticeInputStart()
            }
        }
    }

    private var dialogJob: Job? = null
    private var flashJob: Deferred<Unit>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.animateView.resetInitTime()

        binding.animateView.setGrainAlphaModeIntoWaitCommand(onTimeUpForCommand)
        showDialog("COMMAND CHANNEL OPEN…")
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        min = if (sp.contains(PrefActivity.Key.LEVEL_MIN.name)) sp.getInt(PrefActivity.Key.LEVEL_MIN.name, 0) else 0
        Timber.d("min: $min")

        max = if (sp.contains(PrefActivity.Key.LEVEL_MAX.name)) sp.getInt(PrefActivity.Key.LEVEL_MAX.name, 8) else 8
        Timber.d("max: $max")

        gameMode = if (sp.contains(PrefActivity.Key.GAME_MODE.name)) sp.getInt(PrefActivity.Key.GAME_MODE.name, 0) else 0
        Timber.d("gameMode: $gameMode")

        if (sp.contains(PrefActivity.Key.VIBRATE.name)) doVibrate = sp.getBoolean(PrefActivity.Key.VIBRATE.name, true)
        Timber.d("doVibrate: $doVibrate")

        if (savedInstanceState == null) {
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
                            binding.animateView.setGrainAlphaModeIntoInput(Pair(paths.size + 1, level.getDifficulty()))
                            binding.animateView.addParticle(event.x, event.y)
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                                if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) vibrate()
                            }
                            throughDots.addAll(collision)
                            binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                            binding.animateView.addParticle(event.x, event.y)
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

                            setLeftButton("REDO") {
                                paths.removeAt(paths.lastIndex)
                                binding.dotsView.setDotsState { false }
                                binding.animateView.clearParticle()
                                binding.animateView.setGrainAlphaModeIntoInput(Pair(paths.size, level.getDifficulty()))

                                if (paths.isEmpty()) hideLeftButton()
                            }

                            binding.animateView.apply {
                                setGrainAlphaModeIntoFadeout { binding.dotsView.setDotsState { false } }
                                showPaths(paths.last().mapToPointPathsFromDotPaths(binding.dotsView.getDots()))
                            }
                            if (paths.size >= level.getDifficulty()) binding.animateView.setGrainAlphaModeIntoPrepareAnswer()

                            fromX = -1f
                            fromY = -1f
                            true
                        }
                        else -> true
                    }
                }

                AnimateView.InputState.COMMAND -> {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                            if (binding.animateView.command == null) {
                                clearFlash()
                                flashJob = async { showFlash(0.78f) }
                            }
                            fromX = event.x
                            fromY = event.y
                            binding.animateView.clearParticle()
                            throughDots.clear()
                            binding.dotsView.setDotsState { false }
                            hideDialog()
                            binding.animateView.addParticle(event.x, event.y)
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val collision = binding.dotsView.getCollision(fromX, fromY, event.x, event.y) {
                                if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) vibrate()
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
                                if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0)) vibrate()
                            }
                            throughDots.addAll(collision)
                            binding.dotsView.setDotsState(collision.map { Pair(it, true) })

                            val path = throughDots.convertDotsListToPaths().getNormalizedPaths()
                            binding.animateView.showPaths(path.mapToPointPathsFromDotPaths(binding.dotsView.getDots()))
                            commandMaster.forEach {
                                if (it.match(path)) {
                                    val command = DBInitialData.Shaper.valueOf(it.name)
                                    binding.animateView.command = command
                                    showDialog(command)
                                    clearFlash()
                                    flashJob = async { showFlash(0.78f) }
                                }
                            }
                            binding.animateView.setGrainAlphaModeIntoWaitCommand(onTimeUpForCommand)

                            fromX = -1f
                            fromY = -1f
                            true
                        }

                        else -> true
                    }
                }
            }
        }

        hideLeftButton()
        setRightButton("NEXT") { mainActivity.onNext() }

        val t: Tracker? = (activity.application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    private fun getTouchStatus(): Int =
            when {
                fromX > 0f || fromY > 0f -> MotionEvent.ACTION_DOWN
                else -> MotionEvent.ACTION_UP
            }

    override fun onResume() {
        super.onResume()

        paths.clear()
        spentTimes.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
        dialogJob?.clear()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState?.apply {
            putInt(STATE_ARGS_LEVEL, level)
            putParcelableArrayList(STATE_ARGS_QUESTIONS, ArrayList(questions))
        }
    }

    private fun getAllowableTime(level: Int): Long =
            when (level) {
                in 0..3 -> 20000L
                in 4..8 -> 20000L - 1000L * (level - 3)
                else -> 0L
            }

    private fun setFlashColorWithAlpha(maxAlpha: Float, elapsedTime: Long, final: Boolean, onFinish: () -> Unit = {}) {
        val colorFlash: Int = 0xfff0a328.toInt()
        val pre = 10L
        val main = 670L
        val whole = pre + main
        val finalTime = 250L

        if (elapsedTime > -1L) {
            binding.flashView.apply {
                setBackgroundColor(if (final) Color.WHITE else colorFlash)
                alpha =
                        if (final) {
                            if (elapsedTime < finalTime) 1f
                            else {
                                onFinish()
                                0f
                            }
                        } else {
                            when (elapsedTime) {
                                in 0..pre -> maxAlpha * elapsedTime / pre

                                in pre..whole -> maxAlpha * (1 - (elapsedTime - pre).toFloat() / main)

                                else -> {
                                    onFinish()
                                    0f
                                }
                            }
                        }
            }
        }
    }

    private fun clearFlash() {
        flashJob?.cancel()
        binding.flashView.apply { setBackgroundColor(Color.TRANSPARENT) }
    }

    private suspend fun showFlash(maxAlpha: Float, final: Boolean = false) {
        var finish = false
        val referenceTime: Long = System.currentTimeMillis()

        while (!finish) {
            try {
                val elapsedTime = System.currentTimeMillis() - referenceTime
                uiLaunch { setFlashColorWithAlpha(maxAlpha, elapsedTime, final) { finish = true } }
                delay(10L)
            } catch (e: CancellationException) { finish = true } catch (e: Exception) { Timber.e(e) }
        }
    }

    private fun showFlashForNoticeInputStart() {
        async {
            if (binding.animateView.command != DBInitialData.Shaper.COMPLEX) flashJob = async { showFlash(0.59f) }
            flashJob?.await()
            flashJob = async {showFlash(0.78f) }
            flashJob?.await()
            flashJob = async { showFlash(1f, true) }
            flashJob?.await()
            uiLaunch { clearFlash() }
            async { binding.animateView.setGrainAlphaModeIntoInput(Pair(0, level.getDifficulty())) }
        }
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
            val difficulty = level.getDifficulty()
            binding.animateView
                    .setGrainAlphaModeIntoQuestion(
                            Pair(difficulty + 1 - questions.size, difficulty),
                            getAllowableTime(level),
                            onStartNextQ = { showSequence(questions.drop(1), onComplete) },
                            onStartInput = {
                                setRightButton("BYPASS") {
                                    binding.animateView.setGrainAlphaModeIntoPrepareAnswer()
                                }
                            },
                            onPrepareAnswer = {
                                hideLeftButton()
                                hideRightButton()
                                binding.dotsView.visibility = View.INVISIBLE
                            },
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

    private fun getSequence(id: Long? = null, mode: MainActivity.Mode? = null, level: Int? = null): List<Shaper> {
        fun getLevel(): Int = ((max - min + 1) * Math.random() + min).toInt()

        this.level = level ?: mainActivity.level ?: getLevel().apply { Timber.d("level: $this") }
        val difficulty = this.level.getDifficulty().apply { Timber.d("difficulty: $this") }
        mainActivity.level = this.level

        return when (mode ?: mainActivity.getMode()) {
            MainActivity.Mode.NORMAL -> {
                when (difficulty) {
                    1 -> {
                        realm.where(DBShaper::class.java).count().let {
                            realm.where(DBShaper::class.java)
                                    .findAll()
                                    .toList()
                                    .map { it.parse() }
                                    .let {
                                        listOf(it[id?.toInt() ?: (Math.random() * it.size).toInt().apply { mainActivity.sequenceId = this.toLong() }])
                                    }
                        }
                    }

                    in 2..5 -> {
                        realm.where(Sequence::class.java)
                                .equalTo("size", difficulty)
                                .findAll()
                                .toList()
                                .map { it.message.toList().map { it.parse() } }
                                .let {
                                    it[id?.toInt() ?: (Math.random() * it.size).toInt().apply { mainActivity.sequenceId = this.toLong() }]
                                }
                    }

                    else -> listOf()
                }
            }

            MainActivity.Mode.WEAKNESS -> {
                when (difficulty) {
                    1 -> {
                        realm.where(DBShaper::class.java).let { shapers ->
                            if (id == null) {
                                shapers.greaterThan("examCount", 0).let {
                                    val size = it.count().toInt()
                                    if (size > shapers.count() * 0.8) {
                                        it.findAll().toList()
                                                .sortedBy { it.correctCount.toDouble() / it.examCount }
                                                .take(25)
                                                .map { it.parse() }
                                                .let {
                                                    val index = (Math.random() * 15).toInt()
                                                    val shaper =
                                                            (if (index > it.lastIndex) {
                                                                var shaperId: Long
                                                                do {
                                                                    shaperId = (Math.random() * size).toLong()
                                                                } while (it.map { it.id }.contains(shaperId))
                                                                shapers.findAll().filterNotNull()[shaperId.toInt()].parse()
                                                            } else it[index]).apply { mainActivity.sequenceId = this.id }
                                                    listOf(shaper)
                                                }
                                    } else getSequence(mode = MainActivity.Mode.NORMAL, level = this@MainFragment.level)
                                }
                            } else {
                                listOf(shapers.findAll().map { it.parse() }[id.toInt()])
                            }
                        }
                    }

                    in 2..5 -> {
                        realm.where(Sequence::class.java).equalTo("size", difficulty).let { sequences ->
                            if (id == null) {
                                sequences.greaterThan("examCount", 0).let {
                                    val size = it.count().toInt()
                                    if (size > sequences.count() * 0.8) {
                                        it.findAll().toList()
                                                .sortedBy { it.correctCount.toDouble() / it.examCount }
                                                .take(25)
                                                .map { (it.id to it.message.toList().map { it.parse() }) }
                                                .let {
                                                    val index = (Math.random() * 15).toInt()
                                                    (if (index > it.size - 1) {
                                                        var sequenceId: Long
                                                        do {
                                                            sequenceId = (Math.random() * size).toLong()
                                                        } while (it.map { it.first }.contains(sequenceId))
                                                        sequences.findAll().filterNotNull()[sequenceId.toInt()].let { it.id to it.message.toList().map { it.parse() } }
                                                    } else it[index]).apply { mainActivity.sequenceId = this.first }.second
                                                }
                                    } else getSequence(mode = MainActivity.Mode.NORMAL, level = this@MainFragment.level)
                                }
                            } else {
                                sequences.findAll().filterNotNull()[id.toInt()].let { it.message.toList().map { it.parse() } }
                            }
                        }
                    }

                    else -> listOf()
                }
            }
        }
    }

    private fun addCurrentSpentTime() = spentTimes.add(System.currentTimeMillis() - binding.animateView.getInputStartTime() - spentTimes.sum())

    private fun checkAnswer() =
            mainActivity.transitionForCheckAnswer(
                    questions.mapIndexed { i, q ->
                        if (i < paths.size) Result(q.id, q.match(paths[i]), spentTimes[i])
                        else Result(q.id, false, 0L)
                    },
                    getAllowableTime(level)
            )

    private fun showDialog(command: DBInitialData.Shaper) {
        val message =
                when (command) {
                    DBInitialData.Shaper.COMPLEX -> "COMPLEX HACK"
                    DBInitialData.Shaper.SIMPLE -> "SIMPLE HACK"
                    DBInitialData.Shaper.MORE -> "REQUEST KEY"
                    DBInitialData.Shaper.LESS -> "NO KEY"
                    else -> ""
                }

        if (message.isNotBlank()) showDialog(message)
    }

    private fun showDialog(message: String) {
        if (message.isNotBlank()) {
            binding.dialog.apply {
                val id: Long = (tag as? Long)?.plus(1) ?: 0
                tag = id
                text = message
                visibility = View.VISIBLE

                uiLaunch {
                    delay(1000)
                    if ((tag as? Long) == id) hideDialog()
                }.apply { dialogJob = this }
            }
        }
    }

    private fun hideDialog() {
        binding.dialog.visibility = View.GONE
        if (getTouchStatus() != MotionEvent.ACTION_DOWN) {
            binding.dotsView.setDotsState { false }
            binding.animateView.clearParticle()
        }
    }
}