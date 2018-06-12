package jp.org.example.geckour.glyph.ui

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
import io.realm.Realm
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.FragmentMainBinding
import jp.org.example.geckour.glyph.db.DBInitialData
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.ui.MainActivity.Companion.hacks
import jp.org.example.geckour.glyph.ui.model.Result
import jp.org.example.geckour.glyph.ui.model.ResultDetail
import jp.org.example.geckour.glyph.ui.view.AnimateView
import jp.org.example.geckour.glyph.ui.view.Shaper
import jp.org.example.geckour.glyph.util.*
import kotlinx.coroutines.experimental.*
import timber.log.Timber
import java.util.*
import jp.org.example.geckour.glyph.db.model.Shaper as DBShaper

class MainFragment : Fragment() {

    companion object {
        val tag: String = MainFragment::class.java.simpleName

        fun newInstance(): MainFragment = MainFragment()

        private const val STATE_ARGS_LEVEL = "state_args_level"
        private const val STATE_ARGS_QUESTIONS = "state_args_questions"
    }

    private val mainActivity: MainActivity by lazy { activity as MainActivity }

    private lateinit var binding: FragmentMainBinding
    private lateinit var realm: Realm
    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }

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

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_main, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.animateView.apply {
            resetInitTime()
            setGrainAlphaModeIntoWaitCommand(onTimeUpForCommand)
        }
        showDialog(getString(R.string.message_opening_command_channel))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        min = sharedPreferences.getIntValue(Key.LEVEL_MIN)

        max = sharedPreferences.getIntValue(Key.LEVEL_MAX)

        gameMode = sharedPreferences.getIntValue(Key.GAME_MODE)

        doVibrate = sharedPreferences.getBooleanValue(Key.VIBRATE)

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
                AnimateView.InputState.DISABLED -> return@setOnTouchListener false

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
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val collision = binding.dotsView
                                    .getCollision(fromX, fromY, event.x, event.y) {
                                        if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0))
                                            activity?.vibrate()
                                    }

                            throughDots.addAll(collision)
                            binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                            binding.animateView.addParticle(event.x, event.y)

                            fromX = event.x
                            fromY = event.y
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            addCurrentSpentTime()

                            val collision = binding.dotsView
                                    .getCollision(fromX, fromY, event.x, event.y) {
                                        if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0))
                                            activity?.vibrate()
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
                        }
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
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val collision = binding.dotsView
                                    .getCollision(fromX, fromY, event.x, event.y) {
                                        if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0))
                                            activity?.vibrate()
                                    }

                            throughDots.addAll(collision)
                            binding.dotsView.setDotsState(collision.map { Pair(it, true) })
                            binding.animateView.addParticle(event.x, event.y)

                            fromX = event.x
                            fromY = event.y
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                            val collision = binding.dotsView
                                    .getCollision(fromX, fromY, event.x, event.y) {
                                        if (doVibrate && (throughDots.isEmpty() || it.count { it != throughDots.last() } > 0))
                                            activity?.vibrate()
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
                        }
                    }
                }
            }

            return@setOnTouchListener true
        }

        hideLeftButton()
        setRightButton("NEXT") { mainActivity.onNext() }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putInt(STATE_ARGS_LEVEL, level)
            putParcelableArrayList(STATE_ARGS_QUESTIONS, ArrayList(questions))
        }
    }

    private fun getAllowableTime(level: Int = this.level): Long =
            when (level) {
                in 0..3 -> 20000L
                in 4..8 -> 20000L - 1000L * (level - 3)
                else -> 0L
            }

    private fun setFlashColorWithAlpha(maxAlpha: Float,
                                       elapsedTime: Long,
                                       final: Boolean,
                                       onFinish: () -> Unit = {}) {
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
                            if (elapsedTime < finalTime)
                                1f
                            else {
                                onFinish()
                                0f
                            }
                        } else {
                            when (elapsedTime) {
                                in 0..pre -> {
                                    maxAlpha * elapsedTime / pre
                                }

                                in pre..whole -> {
                                    maxAlpha * (1 - (elapsedTime - pre).toFloat() / main)
                                }

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
                ui { setFlashColorWithAlpha(maxAlpha, elapsedTime, final) { finish = true } }
                delay(10L)
            } catch (e: CancellationException) {
                finish = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun showFlashForNoticeInputStart() {
        launch {
            if (binding.animateView.command != DBInitialData.Shaper.COMPLEX) {
                async { showFlash(0.59f) }.await()
            }
            async { showFlash(0.78f) }.await()
            async { showFlash(1f, true) }.await()
            ui { clearFlash() }
            async { binding.animateView.setGrainAlphaModeIntoInput(Pair(0, level.getDifficulty())) }
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

    private fun showSequence(questions: List<Shaper> = this.questions,
                             onComplete: () -> Unit = {}) {
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
                            shaper.dots.convertDotsListToPaths()
                                    .getNormalizedPaths()
                                    .mapToPointPathsFromDotPaths(binding.dotsView.getDots())
                    )
                }
            }

    private fun getSequence(id: Long? = null,
                            mode: MainActivity.Mode? = null,
                            level: Int? = null): List<Shaper> {

        fun getLevel(): Int = ((max - min + 1) * Math.random() + min).toInt()

        this.level = level
                ?: mainActivity.level
                ?: getLevel()

        mainActivity.level = this.level

        val difficulty = this.level.getDifficulty()

        return if (id != null) {
            when (difficulty) {
                1 -> {
                    listOf(realm.where(DBShaper::class.java)
                            .findAll()
                            .first { it.id == id }
                            .parse())
                }

                in 2..5 -> {
                    realm.where(Sequence::class.java)
                            .equalTo("size", difficulty)
                            .findAll()
                            .firstOrNull { it.id == id }?.let {
                                it.message.toList().map { it.parse() }
                            } ?: emptyList()
                }

                else -> emptyList()
            }
        } else {
            when (mode ?: mainActivity.getMode()) {
                MainActivity.Mode.NORMAL -> {
                    when (difficulty) {
                        1 -> {
                            realm.where(DBShaper::class.java)
                                    .findAll()
                                    .toList()
                                    .map { it.parse() }
                                    .let { shaperList ->
                                        val shaper = shaperList.random()
                                        mainActivity.sequenceId = shaper.id

                                        return@let listOf(shaper)
                                    }
                        }

                        in 2..5 -> {
                            realm.where(Sequence::class.java)
                                    .equalTo("size", difficulty)
                                    .findAll()
                                    .toList()
                                    .let { sequenceList ->
                                        val sequence = sequenceList.random()
                                        mainActivity.sequenceId = sequence.id
                                        return@let sequence.message.map { it.parse() }
                                    }
                        }

                        else -> listOf()
                    }
                }

                MainActivity.Mode.WEAKNESS -> {
                    when (difficulty) {
                        1 -> {
                            val whole = realm.where(DBShaper::class.java)

                            whole.greaterThan("examCount", 0).let {
                                val shaperList = it.findAll().toList()
                                val count = shaperList
                                        .count { it.correctCount.toDouble() / it.examCount < 0.25 }

                                return@let if (count > whole.count() * 0.2) {
                                    shaperList
                                            .sortedBy { it.correctCount.toDouble() / it.examCount }
                                            .take(25)
                                            .map { it.parse() }
                                            .let {
                                                val shaper = it.random()
                                                mainActivity.sequenceId = shaper.id

                                                listOf(shaper)
                                            }
                                } else {
                                    getSequence(
                                            mode = MainActivity.Mode.NORMAL,
                                            level = this@MainFragment.level)
                                }
                            }
                        }

                        in 2..5 -> {
                            val whole = realm.where(Sequence::class.java)
                                    .equalTo("size", difficulty)

                            whole.greaterThan("examCount", 0).let {
                                val sequenceList = it.findAll().toList()
                                val count = sequenceList
                                        .count { it.correctCount.toDouble() / it.examCount < 0.25 }

                                return@let if (count > whole.count() * 0.2) {
                                    sequenceList
                                            .sortedBy { it.correctCount.toDouble() / it.examCount }
                                            .take(25)
                                            .map {
                                                (it.id
                                                        to it.message.toList().map { it.parse() })
                                            }.let list@{ pairList ->
                                                val sequence = pairList.random()
                                                mainActivity.sequenceId = sequence.first

                                                return@list sequence.second
                                            }
                                } else {
                                    getSequence(
                                            mode = MainActivity.Mode.NORMAL,
                                            level = this@MainFragment.level)
                                }
                            }
                        }

                        else -> emptyList()
                    }
                }
            }
        }
    }

    private fun addCurrentSpentTime() {
        spentTimes.add(
                System.currentTimeMillis()
                        - binding.animateView.getInputStartTime()
                        - spentTimes.sum()
        )
    }

    private fun checkAnswer() {
        mainActivity.transitionForCheckAnswer(
                Result(
                        questions.mapIndexed { i, q ->
                            if (i < paths.size) ResultDetail(q.id, null, q.match(paths[i]), spentTimes[i], null)
                            else ResultDetail(q.id, null, false, 0L, null)
                        },
                        hacks
                ),
                getAllowableTime()
        )
    }

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

                ui {
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