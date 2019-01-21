package jp.org.example.geckour.glyph.ui.view

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.App.Companion.scale
import jp.org.example.geckour.glyph.BuildConfig
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.db.DBInitialData
import jp.org.example.geckour.glyph.util.clear
import jp.org.example.geckour.glyph.util.toTimeStringPair
import jp.org.example.geckour.glyph.util.ui
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class AnimateView : View, CoroutineScope {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    enum class State {
        DEFAULT,
        WAIT_COMMAND,
        QUESTION,
        PREPARE_INPUT,
        INPUT,
        FADEOUT,
        PREPARE_ANSWER,
        DICTIONARY,
        INVISIBLE
    }

    enum class InputState {
        ENABLED,
        DISABLED,
        COMMAND
    }

    private val paint = Paint().apply { isAntiAlias = true }
    private val drawAnswerLength: Long by lazy {
        when (command) {
            DBInitialData.Shaper.COMPLEX -> 440L
            DBInitialData.Shaper.SIMPLE -> 2600L
            else -> 1300L
        }
    }

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val coda: Typeface? = ResourcesCompat.getFont(context, R.font.coda)

    private var state = State.DEFAULT
    var command: DBInitialData.Shaper? = null
        set(value) {
            if (value == DBInitialData.Shaper.COMPLEX || value == DBInitialData.Shaper.SIMPLE)
                field = value
        }
    private var showName = true
    private val shaperName: ArrayList<String> = ArrayList()
    private val locus: ArrayList<Particle> = ArrayList()
    private var onTimeUpForCommand: () -> Unit = {}
    private var _onFadeStart: () -> Unit = {}
    private var onFadeStart: () -> Unit = {}
    private var _onStartNextQ: () -> Unit = {}
    private var onStartNextQ: () -> Unit = {}
    private var onStartInput: () -> Unit = {}
    private var onPrepareAnswer: () -> Unit = {}
    private var onTransitionToCheckAnswer: () -> Unit = {}
    private var grainAlpha = 0
    private val particleInterval = 20.0 * scale

    private val commandWaitTime: Long = 2000L
    private val marginTime: Long = 900L
    private var initTime = System.currentTimeMillis()
    private var now = initTime
    private var allowableTime = -1L
    private var referenceTime = -1L
    private var elapsedTime = -1L
        get() = now - referenceTime
    private var timeInQ = -1L
    private var inputStartTime = -1L
    private var spentTime = -1L
    private var progress: Pair<Int, Int> = Pair(0, 0)
    private val hexWidth: Int = (scale * 110).toInt()
    private val hexMargin: Float = hexWidth * (Math.cos(Math.PI / 6) - 1f).toFloat() * 0.5f
    private val hexagons: Array<PointF> by lazy { Array(progress.second) { getHexagonPosition(it) } }

    private lateinit var grainImg: Bitmap
    private lateinit var strongHexImg: Bitmap
    private lateinit var normalHexImg: Bitmap
    private lateinit var weakHexImg: Bitmap

    private var ready: Boolean = false

    private var onResourcesReady: suspend () -> Unit = {}

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        launch {
            ready = async { initResources() }.await()
            ui { onResourcesReady() }

            var cancelled = false
            while (!cancelled) {
                try {
                    if (height > 0) postInvalidate()
                    delay(10)
                } catch (e: CancellationException) {
                    cancelled = true
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        now = System.currentTimeMillis()

        if (ready) canvas?.let {
            when (state) {
                State.WAIT_COMMAND -> {
                    if (elapsedTime > commandWaitTime) onTimeUpForCommand()
                    drawRemain(it, allowableTime)
                    drawParticle(it)
                }

                State.QUESTION -> {
                    if (now - initTime > marginTime) {
                        timeInQ = (elapsedTime % drawAnswerLength).apply {
                            onStartNextQ =
                                    when {
                                        timeInQ > -1 && this - timeInQ < 0 -> {
                                            onStartNextQ()
                                            ({})
                                        }
                                        else ->
                                            _onStartNextQ
                                    }
                        }

                        drawQuestionProgress(it)
                        drawRemain(it, 0L)
                        if (showName) drawShaperName(it)
                        drawParticle(it)
                    } else {
                        referenceTime = now
                        drawQuestionProgress(it, 0)
                    }
                }

                State.PREPARE_INPUT -> {
                    drawRemain(it, 0L)
                }

                State.INPUT -> {
                    if (inputStartTime > -1L) {
                        onStartInput()
                        onStartInput = {}
                        if (now - inputStartTime > allowableTime) setGrainAlphaModeIntoPrepareAnswer(true)

                        drawQuestionProgress(it)
                        drawRemain(it, now - inputStartTime)
                        drawParticle(it)
                    }
                }

                State.FADEOUT -> {
                    if (now - inputStartTime > allowableTime) setGrainAlphaModeIntoPrepareAnswer(true)


                    if (referenceTime > -1L) {
                        drawQuestionProgress(it)
                        drawRemain(it, now - inputStartTime)
                        drawParticle(it)

                        val tol = 500
                        onFadeStart =
                                if (elapsedTime < tol) {
                                    _onFadeStart
                                } else {
                                    onFadeStart()
                                    ({})
                                }
                    }
                }

                State.PREPARE_ANSWER -> {
                    when {
                        elapsedTime < 500L -> {
                            drawRemain(it, spentTime)
                            drawQuestionProgress(it)
                            drawParticle(it)
                        }

                        elapsedTime in 500L..1000L -> {
                            clearParticle()
                            onPrepareAnswer()
                            onPrepareAnswer = {}
                        }

                        else -> {
                            onTransitionToCheckAnswer()
                            onTransitionToCheckAnswer = {}
                        }
                    }
                }

                State.DICTIONARY -> {
                    drawShaperName(it)
                    drawParticle(it)
                }

                else -> {
                }
            }

            if (BuildConfig.DEBUG) drawDebugMessage(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        recycleResources()
        job.clear()
    }

    fun setOnResourcesReady(onResourcesReady: suspend () -> Unit) {
        this.onResourcesReady = onResourcesReady
    }

    private fun initResources(): Boolean {
        val grainDiam = (35.0 * scale).toInt()
        grainImg =
                BitmapFactory.decodeResource(resources, R.drawable.particle, BitmapFactory.Options().apply { inMutable = true }).let {
                    Bitmap.createScaledBitmap(it, grainDiam, grainDiam, false)
                }

        strongHexImg =
                BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_strong).let {
                    Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
                }
        normalHexImg =
                BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal).let {
                    Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
                }
        weakHexImg =
                BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_weak).let {
                    Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
                }

        return true
    }

    private fun recycleResources() {
        grainImg.recycle()
        strongHexImg.recycle()
        normalHexImg.recycle()
        weakHexImg.recycle()
    }

    fun resetInitTime(initTime: Long? = null): Long {
        this.initTime = initTime ?: now
        return this.initTime
    }

    internal fun getInputStartTime(): Long = this.inputStartTime

    private val remainingHeight: Float by lazy { height * 0.6f - width * 0.4875f }

    private fun drawRemain(canvas: Canvas, elapsedTime: Long) {
        val remainTime: Long = allowableTime - elapsedTime

        fun getBarRect(): RectF {
            val halfWidth = width * 0.35f * remainTime / allowableTime
            val halfHeight = remainingHeight * 0.012f
            val center = PointF(width * 0.5f, remainingHeight * 2f / 3)

            return RectF(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
        }

        fun getRemainInputTimeCenterRect(paint: Paint, divider: String = ":"): Rect {
            val baseCenter = Point(width ushr 1, (remainingHeight * (2f / 3 - 0.036)).toInt())

            return Rect().apply {
                paint.getTextBounds(divider, 0, 1, this)
                val halfWidth = width() ushr 1

                left = baseCenter.x - halfWidth
                top = baseCenter.y - height()
                right = baseCenter.x + halfWidth
                bottom = baseCenter.y
            }
        }

        fun drawRemainInputTimeBar() {
            canvas.drawRect(getBarRect(), paint.apply {
                style = Paint.Style.FILL
                color = if (state == State.INPUT || state == State.FADEOUT) 0xfff5b316.toInt() else 0x70f5b316
            })
        }

        fun drawRemainInputTime() {
            paint.apply {
                textSize = remainingHeight * 0.15f
                color = if (state == State.INPUT || state == State.FADEOUT) 0xfff0d916.toInt() else 0x70f0d916
                typeface = coda
            }

            val rect = getRemainInputTimeCenterRect(paint)
            val timeStringPair = remainTime.toTimeStringPair()

            canvas.apply {
                drawText(timeStringPair.first, rect.left.toFloat(), rect.exactCenterY(), paint.apply { textAlign = Paint.Align.RIGHT })
                drawText(":", rect.exactCenterX(), rect.exactCenterY(), paint.apply { textAlign = Paint.Align.CENTER })
                drawText(timeStringPair.second, rect.right.toFloat(), rect.exactCenterY(), paint.apply { textAlign = Paint.Align.LEFT })
            }
        }

        drawRemainInputTimeBar()
        drawRemainInputTime()
    }

    private fun drawParticle(canvas: Canvas) {
        paint.alpha = getGrainAlpha().apply { grainAlpha = this }
        synchronized(locus) {
            for (particle in locus) {
                particle.move(canvas, paint)
            }
        }
        paint.alpha = 255
    }

    private fun getHexagonPosition(index: Int): PointF {
        val indexForCalc = index - (progress.second - 1) * 0.5f
        return PointF(
                width * 0.5f + (hexWidth + hexMargin) * indexForCalc - hexWidth * 0.5f,
                remainingHeight / 3 - hexWidth * 0.5f
        )
    }

    private fun drawQuestionProgress(canvas: Canvas, numerator: Int? = null) {
        val n = (numerator ?: progress.first) - 1
        if (n > -2) {
            when (state) {
                State.QUESTION -> {
                    paint.colorFilter = PorterDuffColorFilter(Color.rgb(240, 150, 40), PorterDuff.Mode.SRC_ATOP)
                    hexagons.forEachIndexed { i, pointF ->
                        if (i == n)
                            canvas.drawBitmap(strongHexImg, pointF.x, pointF.y, paint)
                        else
                            canvas.drawBitmap(weakHexImg, pointF.x, pointF.y, paint)
                    }
                }

                State.INPUT, State.FADEOUT, State.PREPARE_ANSWER -> {
                    paint.colorFilter = PorterDuffColorFilter(Color.rgb(2, 255, 197), PorterDuff.Mode.SRC_ATOP)
                    hexagons.forEachIndexed { i, pointF ->
                        when (i) {
                            in 0..(n - 1) -> canvas.drawBitmap(normalHexImg, pointF.x, pointF.y, paint)
                            n -> canvas.drawBitmap(strongHexImg, pointF.x, pointF.y, paint)
                            else -> canvas.drawBitmap(weakHexImg, pointF.x, pointF.y, paint)
                        }
                    }
                }

                else -> {
                }
            }

            paint.colorFilter = null
        }
    }

    private fun drawShaperName(canvas: Canvas, names: List<String> = shaperName) {
        if (names.isEmpty()) return

        val fontSize = remainingHeight * 0.15f
        val baseLine = remainingHeight * (2f / 3 + 0.04f) + fontSize
        val margin = remainingHeight * 0.026f

        names.reversed().forEachIndexed { i, name ->
            canvas.drawText(name, width.toFloat() / 2, baseLine - (fontSize + margin) * (names.lastIndex - i), paint.apply {
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
                style = Paint.Style.STROKE
                textSize = fontSize
                typeface = coda
            })
        }
    }

    fun showPaths(paths: List<Pair<PointF, PointF>>) {
        clearParticle()
        if (paths.isEmpty()) return

        paths.forEach { path ->
            val lenX = (path.second.x - path.first.x).toDouble()
            val lenY = (path.second.y - path.first.y).toDouble()
            val distance = Math.sqrt((Math.pow(lenX, 2.0) + Math.pow(lenY, 2.0))).toFloat()
            val dX = particleInterval * lenX / distance
            val dY = particleInterval * lenY / distance

            synchronized(locus) {
                (0 until (distance / particleInterval).toInt()).forEach {
                    addParticle((path.first.x + dX * it).toFloat(), (path.first.y + dY * it).toFloat(), Particle.Phase.CONVERGING)
                }
            }
        }
        synchronized(locus) { addParticle(paths.last().second.x, paths.last().second.y, Particle.Phase.CONVERGING) }
    }

    private fun getGrainAlpha(mode: State = state): Int =
            when (mode) {
                State.DEFAULT -> 255

                State.WAIT_COMMAND -> 255

                State.QUESTION -> {
                    if (now - initTime > marginTime) {
                        val phase = longArrayOf((drawAnswerLength * 0.2).toLong(), (drawAnswerLength * 0.7).toLong())

                        when (timeInQ) {
                            in 0 until phase[0] -> (255 * timeInQ.toDouble() / phase[0]).toInt()
                            in phase[0] until phase[1] -> 255
                            in phase[1]..timeInQ -> (255.0 * (drawAnswerLength - timeInQ) / (drawAnswerLength - phase[1])).toInt()
                            else -> 0
                        }
                    } else 0
                }

                State.INPUT -> 255

                State.FADEOUT -> {
                    if (referenceTime > -1L) {
                        val keep = 500L
                        val fade = 500L

                        when {
                            elapsedTime < keep -> 255
                            keep < elapsedTime && elapsedTime < keep + fade -> {
                                onFadeStart()
                                onFadeStart = {}
                                (255.0 * (fade - (elapsedTime - keep)) / fade).toInt()
                            }
                            else -> 0
                        }
                    } else 255
                }

                State.PREPARE_ANSWER -> grainAlpha

                State.DICTIONARY -> 255

                else -> 0
            }

    fun setShaperName(names: List<String>) {
        this.shaperName.apply {
            clear()
            addAll(names)
        }
        this.showName = true
    }

    fun resetShaperName() {
        this.shaperName.clear()
        this.showName = false
    }

    private fun setState(state: State) {
        this.state = state
    }

    fun getInputState(): InputState =
            when (this.state) {
                State.INPUT, State.FADEOUT, State.DICTIONARY -> InputState.ENABLED
                State.WAIT_COMMAND -> InputState.COMMAND
                else -> InputState.DISABLED
            }

    fun setGrainAlphaModeIntoWaitCommand(onTimeUpForCommand: () -> Unit) {
        this.onTimeUpForCommand = onTimeUpForCommand
        setState(State.WAIT_COMMAND)
        referenceTime = now
    }

    fun setGrainAlphaModeIntoQuestion(progress: Pair<Int, Int>, allowableTime: Long, onStartNextQ: () -> Unit = {}, onStartInput: () -> Unit, onPrepareAnswer: () -> Unit, onTransitionToCheckAnswer: () -> Unit = {}) {
        this.progress = progress
        this.allowableTime = allowableTime
        _onStartNextQ = onStartNextQ
        this.onStartNextQ = _onStartNextQ
        this.onStartInput = onStartInput
        this.onPrepareAnswer = onPrepareAnswer
        this.onTransitionToCheckAnswer = onTransitionToCheckAnswer

        setState(State.QUESTION)
        referenceTime = now
    }

    fun setGrainAlphaModeIntoPrepareInput() {
        clearParticle()

        setState(State.PREPARE_INPUT)

        referenceTime = now
    }

    fun setGrainAlphaModeIntoInput(progress: Pair<Int, Int>) {
        setState(State.INPUT)
        if (inputStartTime < 0L) inputStartTime = now
        this.progress = progress
    }

    fun setGrainAlphaModeIntoFadeout(onFadeStart: () -> Unit = {}) {
        setState(State.FADEOUT)
        _onFadeStart = onFadeStart
        this.onFadeStart = _onFadeStart
        referenceTime = now
    }

    fun setGrainAlphaModeIntoPrepareAnswer(timeUp: Boolean = false) {
        setState(State.PREPARE_ANSWER)
        spentTime = if (timeUp) allowableTime else now - inputStartTime
        referenceTime = now
    }

    fun setGrainAlphaModeIntoDictionary() {
        setState(State.DICTIONARY)
    }

    @Deprecated("This method is redundant.")
    fun resetGrainAlphaMode() {
        setState(State.DEFAULT)
        referenceTime = -1L

        onFadeStart = {}
    }

    fun addParticle(x: Float, y: Float, phase: Particle.Phase? = null) {
        locus.lastOrNull { !it.drawn }?.let { last ->
            val lenX = (x - last.x).toDouble()
            val lenY = (y - last.y).toDouble()
            val distance = Math.sqrt(Math.pow(lenX, 2.0) + Math.pow(lenY, 2.0))
            val dX = particleInterval * lenX / distance
            val dY = particleInterval * lenY / distance

            synchronized(locus) {
                (1..(distance / particleInterval).toInt()).forEach {
                    locus.add(Particle((last.x + dX * it).toFloat(), (last.y + dY * it).toFloat(), grainImg, width, phase).apply { drawn = true })
                    last.drawn = true
                }
                locus.last().drawn = false
            }
        } ?: synchronized(locus) { locus.add(Particle(x, y, grainImg, width, phase)) }
    }

    fun clearParticle() = synchronized(locus) { locus.clear() }

    private fun getDebugMessage() = "$elapsedTime, $state, alpha:$grainAlpha"

    private fun drawDebugMessage(canvas: Canvas, message: String = getDebugMessage()) =
            canvas.drawText(message, 20f, 60f, paint.apply {
                color = Color.GREEN
                textSize = 40f
                textAlign = Paint.Align.LEFT
            })

    override fun performClick(): Boolean = super.performClick()
}