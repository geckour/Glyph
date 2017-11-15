package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.BuildConfig
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.util.format
import jp.org.example.geckour.glyph.view.model.Particle
import jp.org.example.geckour.glyph.view.model.Particle.Companion.grainImg
import timber.log.Timber
import kotlin.concurrent.thread

class AnimateView: View {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

    enum class State {
        DEFAULT,
        QUESTION,
        PREPARE_INPUT,
        INPUT,
        FADEOUT,
        PREPARE_ANSWER,
        DICTIONARY,
        INVISIBLE
    }

    private val paint = Paint()
    private val drawAnswerLength: Long by lazy { 1200L } // TODO: COMPLEXモードか否かで切り替え

    private var state = State.DEFAULT
    private var inputEnabled = false
    private var showName = true
    private val shaperName: ArrayList<String> = ArrayList()
    private val locus: ArrayList<Particle> = ArrayList()
    private var _onFadeStart: () -> Unit = {}
    private var onFadeStart: () -> Unit = {}
    private var _onStartNextQ: () -> Unit = {}
    private var onStartNextQ: () -> Unit = {}
    private var onPrepareAnswer: () -> Unit = {}
    private var onTransitionToCheckAnswer: () -> Unit = {}
    private var grainAlpha = 0

    private val scale: Float by lazy { height.toFloat() / 1280 }
    private val marginTime: Long = 900
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
    private val hexMargin: Float by lazy { width * 0.05f }
    private val hexagons: Array<PointF> by lazy { Array(progress.second) { getHexagonPosition(it) } }

    private val strongHexImg: Bitmap by lazy {
        val hexWidth = (width * 0.1f).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.dot_t).let { // FIXME
            Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
        }
    }
    private val normalHexImg: Bitmap by lazy {
        val hexWidth = (width * 0.1f).toInt()
        BitmapFactory.decodeResource(resources, R.drawable.particle).let { // FIXME
            Bitmap.createScaledBitmap(it, hexWidth, hexWidth, false)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (height > 0) {
            val grainDiam = (32.0 * scale).toInt()
            grainImg = BitmapFactory.decodeResource(resources, R.drawable.particle).let {
                Bitmap.createScaledBitmap(it, grainDiam, grainDiam, false)
            }
        }

        thread {
            while (true) {
                postInvalidate()
                try {
                    Thread.sleep(10)
                } catch (e: Exception) { Timber.e(e) }
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        now = System.currentTimeMillis()
        
        canvas?.let {
            when (state) {
                State.QUESTION -> {
                    if (now - initTime > marginTime) {
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

    fun resetInitTime(initTime: Long? = null): Long {
        this.initTime = initTime ?: now
        return this.initTime
    }

    private fun drawRemain(canvas: Canvas, elapsedTime: Long) {
        val remainTime = allowableTime - elapsedTime
        val remainingHeight = height * 0.6f - (width * 0.4f + 0.1f / 3)

        fun Paint.setForRemainInputTime(align: Paint.Align = Paint.Align.CENTER) =
                this.apply {
                    textSize = remainingHeight * 0.2f
                    color = Color.rgb(220, 190, 50)
                    textAlign = align
                    typeface = Typeface.create("coda", Typeface.NORMAL)
                }

        fun getBarRect(): RectF {
            val halfWidth = width * 0.35f * remainTime / allowableTime
            val halfHeight = remainingHeight * 0.03f
            val center = PointF(width.toFloat() / 2, remainingHeight * 0.375f)

            return RectF(center.x - halfWidth, center.y - halfHeight, center.x + halfWidth, center.y + halfHeight)
        }

        fun getRemainInputTimeCenterRect(paint: Paint, divider: String = ":"): Rect {
            val center = Point((width.toDouble() / 2).toInt(), (remainingHeight * 0.25).toInt())

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
            val sec = remainTime / 1000
            val millis = remainTime % 1000
            val rect = getRemainInputTimeCenterRect(paint.setForRemainInputTime())

            canvas.apply {
                drawText("$sec", rect.left.toFloat(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.RIGHT))
                drawText(":", rect.exactCenterX(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.CENTER))
                drawText(millis.format(2).take(2), rect.right.toFloat(), rect.exactCenterY(), paint.setForRemainInputTime(Paint.Align.LEFT))
            }
        }

        drawRemainInputTimeBar()
        drawRemainInputTime()
    }

    private fun drawParticle(canvas: Canvas) {
        grainAlpha = calcGrainAlpha()
        Particle.setGrainAlpha(grainAlpha)

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
                height * 0.6f - (width * 0.4f + 0.1f / 3) - hexWidth - hexMargin * 2f
        )
    }

    private fun drawQuestionProgress(canvas: Canvas, numerator: Int? = null) {
        val n = numerator ?: progress.first
        if (n > -1) {
            fun draw() {
                hexagons.forEachIndexed { i, pointF ->
                    if (i + 1 > n)
                        canvas.drawBitmap(normalHexImg, pointF.x, pointF.y, paint)
                    else
                        canvas.drawBitmap(strongHexImg, pointF.x, pointF.y, paint)
                }

                paint.colorFilter = null
            }

            when (state) {
                State.QUESTION -> {
                    paint.colorFilter = PorterDuffColorFilter(Color.rgb(240, 150, 40), PorterDuff.Mode.SRC_ATOP)
                }

                State.INPUT, State.FADEOUT, State.PREPARE_ANSWER -> {
                    paint.colorFilter = PorterDuffColorFilter(Color.rgb(2, 255, 197), PorterDuff.Mode.SRC_ATOP)
                }

                else -> {}
            }

            draw()
        }
    }

    private fun drawShaperName(canvas: Canvas, names: List<String> = shaperName) {
        if (names.isEmpty()) return

        val remainingArea = height * 0.6f - (width * 0.4f + 0.1f / 3)
        val baseLine = remainingArea * 0.75f
        val fontSize = remainingArea * 0.15f
        val margin = remainingArea * 0.05f

        names.forEachIndexed { i, name ->
            canvas.drawText(name, width.toFloat() / 2, baseLine - (fontSize + margin) * (names.lastIndex - i), paint.apply {
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
                style = Paint.Style.STROKE
                textSize = fontSize
                typeface = Typeface.create("coda", Typeface.NORMAL)
            })
        }
    }

    fun isInputEnabled() = this.inputEnabled

    fun showPaths(paths: List<Pair<PointF, PointF>>) {
        clearParticle()

        val interval = 20.0 * scale
        paths.forEach {
            val diffX = it.second.x - it.first.x
            val diffY = it.second.y - it.first.y
            val length = Math.sqrt((diffX * diffX + diffY * diffY).toDouble()).toFloat()
            val uVx = diffX / length
            val uVy = diffY / length

            val p = PointF(it.first.x, it.first.y)

            synchronized(locus) {
                val dX = uVx * interval
                val dY = uVy * interval
                val dL = Math.sqrt(dX * dX + dY * dY).toFloat()

                var totalLength = 0f
                while (totalLength <= length) {
                    addParticle(p.x, p.y, 1)
                    p.set(p.x + dX.toFloat(), p.y + dY.toFloat())

                    totalLength += dL
                }
            }
        }
    }

    private fun calcGrainAlpha(mode: State = state): Int =
            when (mode) {
                State.DEFAULT -> 255

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
            val pre = 10
            val main = 670
            val whole = pre + main

            if (elapsedTime > -1L) {
                val seq: Long = elapsedTime / main
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
                        Color.argb(alpha.toInt(), 255, 255, 255)
                    }
                    else -> {
                        onFinish()
                        Color.TRANSPARENT
                    }
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

    fun setGrainAlphaModeIntoQuestion(progress: Pair<Int, Int>, allowableTime: Long, onStartNextQ: () -> Unit = {}, onPrepareAnswer: () -> Unit, onTransitionToCheckAnswer: () -> Unit = {}) {
        this.progress = progress
        this.allowableTime = allowableTime
        _onStartNextQ = onStartNextQ
        this.onStartNextQ = _onStartNextQ
        this.onPrepareAnswer = onPrepareAnswer
        this.onTransitionToCheckAnswer = onTransitionToCheckAnswer

        setState(State.QUESTION)
        inputEnabled = false
        referenceTime = now
    }

    fun setGrainAlphaModeIntoPrepareInput() {
        clearParticle()

        setState(State.PREPARE_INPUT)
        inputEnabled = false

        referenceTime = now
    }

    fun setGrainAlphaModeIntoInput(progress: Pair<Int, Int>) {
        setState(State.INPUT)
        inputEnabled = true
        if (inputStartTime < 0L) inputStartTime = now
        this.progress = progress
    }

    fun setGrainAlphaModeIntoFadeout(onFadeStart: () -> Unit = {}) {
        setState(State.FADEOUT)
        _onFadeStart = onFadeStart
        this.onFadeStart = _onFadeStart
        referenceTime = now
        inputEnabled = true
    }

    fun setGrainAlphaModeIntoPrepareAnswer(timeUp: Boolean = false) {
        setState(State.PREPARE_ANSWER)
        spentTime = if (timeUp) allowableTime else now - inputStartTime
        referenceTime = now
        inputEnabled = false
    }

    fun setGrainAlphaModeIntoDictionary() {
        setState(State.DICTIONARY)
        inputEnabled = true
    }

    @Deprecated("This method is redundant.")
    fun resetGrainAlphaMode() {
        setState(State.DEFAULT)
        referenceTime = -1L
        inputEnabled = false

        onFadeStart = {}
    }

    fun addParticle(x: Float, y: Float, phase: Int? = null) =
            if (width > 0)
                synchronized(locus) {locus.add(Particle(x, y, width, phase)) }
            else false

    fun clearParticle() = synchronized(locus) { locus.clear() }

    private fun getDebugMessage() = "$elapsedTime, $state, inputEnabled: $inputEnabled, alpha:$grainAlpha"

    private fun drawDebugMessage(canvas: Canvas, message: String = getDebugMessage()) =
            canvas.drawText(message, 20f, 60f, paint.apply {
                color = Color.GREEN
                textSize = 40f
            })

    override fun performClick(): Boolean = super.performClick()
}