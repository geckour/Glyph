package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.BuildConfig
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.view.model.Particle
import jp.org.example.geckour.glyph.view.model.Particle.Companion.grainImg
import kotlinx.coroutines.experimental.Job
import timber.log.Timber
import kotlin.concurrent.thread

class AnimateView : View {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

    enum class GrainAlphaMode {
        DEFAULT,
        QUESTION,
        FADEOUT,
        INVISIBLE
    }

    enum class Status {
        PROTECT,
        RELEASE
    }

    private val paint = Paint()
    private val drawAnswerLength: Long = 1200

    private var grainAlphaMode = GrainAlphaMode.DEFAULT
    var status = Status.PROTECT
    private val locus: ArrayList<Particle> = ArrayList()
    private var _onFadeStart: () -> Unit = {}
    private var onFadeStart: () -> Unit = {}
    private var _onStartNextQ: () -> Unit = {}
    private var onStartNextQ: () -> Unit = {}
    private var grainAlpha = 0

    private val marginTime: Long = 900
    private var initTime = System.currentTimeMillis()
    private var now = initTime
    private var releaseTime = -1L
    private var timeInQ = -1L

    private val jobs: ArrayList<Job> = ArrayList()

    private fun timeInQ(): Long = (now - initTime - marginTime) % drawAnswerLength

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (height > 0) {
            val grainDiam = (32.0 * height / 1280).toInt()
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
            drawParticle(it)
            if (BuildConfig.DEBUG) drawDebugMessage(it)
        }
    }

    fun resetInitTime(initTime: Long? = null): Long {
        this.initTime = initTime ?: now
        return this.initTime
    }

    private fun drawParticle(canvas: Canvas) {
        grainAlpha = calcAlpha()
        Particle.setGrainAlpha(grainAlpha)

        synchronized(locus) {
            for (particle in locus) {
                particle.move(canvas, paint)
            }
        }
    }

    fun showPaths(paths: List<Pair<PointF, PointF>>) {
        clearParticle()

        val scale = height.toDouble() / 1280
        val interval = 20 * scale
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

    private fun calcAlpha(mode: GrainAlphaMode = grainAlphaMode, releaseTime: Long = this.releaseTime): Int =
            when (mode) {
                GrainAlphaMode.DEFAULT -> 255

                GrainAlphaMode.QUESTION -> {
                    val phase = longArrayOf((drawAnswerLength * 0.2).toLong(), (drawAnswerLength * 0.7).toLong())
                    timeInQ = timeInQ().apply {
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

                    when (timeInQ) {
                        in 0 until phase[0] -> (255 * timeInQ.toDouble() / phase[0]).toInt()
                        in phase[0] until phase[1] -> 255
                        in phase[1]..timeInQ -> (255.0 * (drawAnswerLength - timeInQ) / (drawAnswerLength - phase[1])).toInt()
                        else -> 0
                    }
                }

                GrainAlphaMode.FADEOUT -> {
                    val tol = 500
                    val fade = 500

                    if (releaseTime > -1L) {
                        val elapsedTime = now - releaseTime

                        if (elapsedTime in tol..tol + fade) {
                            onFadeStart()
                            onFadeStart = {}
                            (255.0 * (tol + fade - elapsedTime) / fade).toInt()
                        } else {
                            onFadeStart = _onFadeStart
                            255
                        }
                    } else 255
                }

                GrainAlphaMode.INVISIBLE -> 0
            }

    fun setGrainAlphaMode(mode: GrainAlphaMode) {
        grainAlphaMode = mode
    }

    fun setGrainAlphaModeIntoQuestion(onStartNextQ: () -> Unit = {}) {
        grainAlphaMode = GrainAlphaMode.QUESTION

        _onStartNextQ = onStartNextQ
        this.onStartNextQ = _onFadeStart
    }

    fun setGrainAlphaModeIntoFadeout(releaseTime: Long, onFadeStart: () -> Unit = {}) {
        grainAlphaMode = GrainAlphaMode.FADEOUT
        this.releaseTime = releaseTime

        _onFadeStart = onFadeStart
        this.onFadeStart = _onFadeStart
    }

    fun resetGrainAlphaMode() {
        grainAlphaMode = GrainAlphaMode.DEFAULT
        this.releaseTime = -1L

        onFadeStart = {}
    }

    fun addParticle(x: Float, y: Float, phase: Int? = null) =
            if (width > 0)
                synchronized(locus) {locus.add(Particle(x, y, width, phase)) }
            else false

    fun clearParticle() = synchronized(locus) { locus.clear() }

    private fun getDebugMessage() = "$timeInQ, $grainAlphaMode, alpha:$grainAlpha"

    private fun drawDebugMessage(canvas: Canvas, message: String = getDebugMessage()) =
            canvas.drawText(message, 20f, 60f, paint.apply {
                color = Color.GREEN
                textSize = 40f
            })

    override fun performClick(): Boolean = super.performClick()
}