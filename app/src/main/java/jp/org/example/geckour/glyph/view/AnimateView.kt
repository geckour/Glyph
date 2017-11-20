package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.App.Companion.coda
import jp.org.example.geckour.glyph.App.Companion.scale
import jp.org.example.geckour.glyph.BuildConfig
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.db.DBInitialData
import jp.org.example.geckour.glyph.util.toTimeStringPair
import jp.org.example.geckour.glyph.view.model.Particle
import timber.log.Timber
import kotlin.concurrent.thread

class AnimateView: View {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

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

    private val paint = Paint()
    private val drawAnswerLength: Long by lazy {
        when (command) {
            DBInitialData.Shaper.COMPLEX -> 600L
            DBInitialData.Shaper.SIMPLE -> 2400L
            else -> 1200L
        }
    }

    private var state = State.DEFAULT
    private var command: DBInitialData.Shaper? = null
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
    private val particleInterval = 22.0 * scale

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
    private val hexWidth: Float by lazy { width * 0.1f }
    private val hexMargin: Float by lazy { width * 0.02f }
    private val hexagons: Array<PointF> by lazy { Array(progress.second) { getHexagonPosition(it) } }

    private val grainImg: Bitmap by lazy {
        val grainDiam = (30.0 * scale).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.particle, BitmapFactory.Options().apply { inMutable = true }).let {
            Bitmap.createScaledBitmap(it, grainDiam, grainDiam, false)
        }
    }
    private val grainPixelsMaster: List<Int> by lazy {
        grainImg.let {
            val w = grainImg.width
            val h = grainImg.height
            IntArray(w * h)
                    .apply { it.getPixels(this, 0, w, 0, 0, w, h) }
                    .toList()
        }
    }
    private val strongHexImg: Bitmap by lazy {
        val hexWidth = (width * 0.1f).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_strong).let {
            Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
        }
    }
    private val normalHexImg: Bitmap by lazy {
        val hexWidth = (width * 0.1f).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal).let {
            Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
        }
    }
    private val weakHexImg: Bitmap by lazy {
        val hexWidth = (width * 0.1f).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_weak).let {
            Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        thread {
            while (true) {
                if (height > 0) postInvalidate()
                try {
                    Thread.sleep(10)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        now = System.currentTimeMillis()
        
        canvas?.let {
            when (state) {
                State.WAIT_COMMAND -> {
                    if (elapsedTime > commandWaitTime) onTimeUpForCommand()
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
                        if (showName) drawShaperName(it)
                        drawParticle(it)
                    } else {
                        referenceTime = now
                        drawQuestionProgress(it, 0)
                    }
                }

                State.PREPARE_INPUT -> {
                    it.drawRect(0.0f, 0.0f, width.toFloat(), height.toFloat(), paint.apply {
                        color = getFlashColor { setGrainAlphaModeIntoInput(progress.copy(first = 0)) }
                        style = Paint.Style.FILL
                    })
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
                        elapsedTime  < 500L -> {
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

                else -> {}
            }

            if (BuildConfig.DEBUG) drawDebugMessage(it)
        }
    }

    private fun setGrainAlpha(alpha: Int) {
        if (alpha !in 0..255) return

        val w = grainImg.width
        val h = grainImg.height
        val subAlpha = 255 - alpha

        grainImg.setPixels(
                grainPixelsMaster.map {
                    val oldAlpha = Color.alpha(it)
                    val newAlpha = oldAlpha - subAlpha
                    (when {
                        newAlpha < 0 -> 0
                        newAlpha > 255 -> 255
                        else -> newAlpha
                    } shl 24) + (it and 0x00ffffff)
                }.toIntArray(), 0, w, 0, 0, w, h)
    }

    fun resetInitTime(initTime: Long? = null): Long {
        this.initTime = initTime ?: now
        return this.initTime
    }

    internal fun getInputStartTime(): Long = this.inputStartTime

    private val remainingHeight: Float by lazy { height * 0.6f - (width * 0.4f + 0.1f / 3) }

    private fun drawRemain(canvas: Canvas, elapsedTime: Long) {
        val remainTime = allowableTime - elapsedTime

        fun Paint.setForRemainInputTime(align: Paint.Align = Paint.Align.CENTER) =
                this.apply {
                    textSize = remainingHeight * 0.2f
                    color = Color.rgb(220, 190, 50)
                    textAlign = align
                    typeface = coda
                }

        fun getBarRect(): RectF {
            val halfWidth = width * 0.35f * remainTime / allowableTime
            val halfHeight = remainingHeight * 0.015f
            val center = PointF(width.toFloat() / 2, remainingHeight * 0.75f)

            return RectF(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
        }

        fun getRemainInputTimeCenterRect(paint: Paint, divider: String = ":"): Rect {
            val center = Point((width.toDouble() / 2).toInt(), (remainingHeight * 0.625).toInt())

            return Rect().apply {
                paint.getTextBounds(divider, 0, 1, this)
                val halfWidth = width() ushr 1
                val halfHeight = height() ushr 1

                left = center.x - halfWidth
                top = center.y - halfHeight
                right = center.x + halfWidth
                bottom = center.y + halfHeight
            }
        }

        fun drawRemainInputTimeBar() {
            canvas.drawRect(getBarRect(), paint.apply {
                color = Color.rgb(220, 190, 50)
            })
        }

        fun drawRemainInputTime() {
            val rect = getRemainInputTimeCenterRect(paint.setForRemainInputTime())
            val timeStringPair = remainTime.toTimeStringPair()

            canvas.apply {
                drawText(timeStringPair.first, rect.left.toFloat(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.RIGHT))
                drawText(":", rect.exactCenterX(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.CENTER))
                drawText(timeStringPair.second, rect.right.toFloat(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.LEFT))
            }
        }

        drawRemainInputTimeBar()
        drawRemainInputTime()
    }

    private fun drawParticle(canvas: Canvas) {
        grainAlpha = calcGrainAlpha()
        setGrainAlpha(grainAlpha)

        synchronized(locus) {
            for (particle in locus) {
                particle.move(canvas, paint)
            }
        }
    }

    private fun getHexagonPosition(index: Int): PointF {
        val indexForCalc = index - (progress.second - 1) * 0.5f
        return PointF(
                width * 0.5f - hexWidth * 0.5f + indexForCalc * (hexWidth + hexMargin),
                remainingHeight * 0.125f - hexWidth * 0.5f
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

                else -> {}
            }

            paint.colorFilter = null
        }
    }

    private fun drawShaperName(canvas: Canvas, names: List<String> = shaperName) {
        if (names.isEmpty()) return

        val fontSize = remainingHeight * 0.15f
        val baseLine = remainingHeight * 0.125f + hexWidth * 0.5f + hexMargin + fontSize
        val margin = remainingHeight * 0.02f

        names.forEachIndexed { i, name ->
            canvas.drawText(name, width.toFloat() / 2, baseLine + (fontSize + margin) * (names.lastIndex - i), paint.apply {
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
    }

    private fun calcGrainAlpha(mode: State = state): Int =
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

                        when  {
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

    private fun getFlashColor(onFinish: () -> Unit): Int {
        if (referenceTime > -1L) {
            val pre = 10L
            val main = 670L
            val whole = pre + main
            val final = 250L // Must be less than whole

            if (elapsedTime > -1L) {
                val seq: Long = elapsedTime / whole + if(command == DBInitialData.Shaper.COMPLEX) 1 else 0
                val timeInSeq: Long = elapsedTime % whole
                return when (seq) {
                    0L -> {
                        val alpha =
                                if (timeInSeq < pre) {
                                    150.0 * timeInSeq / pre
                                } else {
                                    150 * (1 - (timeInSeq - pre).toDouble() / main)
                                }
                        Color.argb(alpha.toInt(), 220, 175, 50)
                    }
                    1L -> {
                        val alpha =
                                if (timeInSeq < pre) {
                                    200.0 * timeInSeq / pre
                                } else {
                                    200 * (1 - (timeInSeq - pre).toDouble() / main)
                                }
                        Color.argb(alpha.toInt(), 220, 175, 50)
                    }
                    2L -> {
                        val alpha =
                                if (timeInSeq < pre) {
                                    255.0 * timeInSeq / pre
                                } else {
                                    255.0
                                }
                        if (timeInSeq < final) Color.argb(alpha.toInt(), 255, 255, 255)
                        else {
                            onFinish()
                            Color.TRANSPARENT
                        }
                    }
                    else -> { Color.TRANSPARENT }
                }
            }
        }
        return Color.TRANSPARENT
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

    fun setCommand(command: DBInitialData.Shaper) {
        this.command = command
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