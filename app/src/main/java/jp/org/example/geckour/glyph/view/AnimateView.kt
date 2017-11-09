package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.view.model.Particle
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

    private val paint = Paint()
    private val locus: ArrayList<Particle> = ArrayList()
    private var canvasHeight: Int = 0
    private var canvasWidth: Int = 0
    private val grainImg = BitmapFactory.decodeResource(resources, R.drawable.particle)
    private var grainAlphaMode = GrainAlphaMode.DEFAULT
    private var releaseTime = -1L
    private var onFadeStart: () -> Unit = {}
    private var _onFadeComplete: () -> Unit = {}
    private var initTime = System.currentTimeMillis()
    private val marginTime: Long = 900
    private val drawAnswerLength: Long = 1200

    private fun timeInQ(): Long = (System.currentTimeMillis() - initTime - marginTime) % drawAnswerLength

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        canvasHeight = bottom - top
        canvasWidth = right - left

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

        canvas?.let { drawParticle(it, paint) }
    }

    fun setInitTime(initTime: Long) {
        this.initTime = initTime
    }

    private fun drawParticle(canvas: Canvas, paint: Paint) {
        synchronized(locus) {
            for (particle in locus) {
                particle.move(canvas, paint, calcSubAlpha(grainAlphaMode, releaseTime))
            }
        }
    }

    fun showPaths(paths: List<Pair<Int, Int>>, dots: Array<PointF>) {
        clearParticle()

        val scale = canvasHeight.toFloat() / 1280
        val interval = 20 * scale
        paths.forEach {
            val diffX = dots[it.second].x - dots[it.first].x
            val diffY = dots[it.second].y - dots[it.first].y
            val length = Math.sqrt((diffX * diffX + diffY * diffY).toDouble()).toFloat()
            val uVx = diffX / length
            val uVy = diffY / length

            val p = PointF(dots[it.first].x, dots[it.first].y)

            synchronized(locus) {
                val dX = uVx * interval
                val dY = uVy * interval
                val dL = Math.sqrt((dX * dX + dY * dY).toDouble()).toFloat()

                var totalLength = 0f
                while (totalLength <= length) {
                    addParticle(p.x, p.y, 1)
                    p.set(p.x + dX, p.y + dY)

                    totalLength += dL
                }
            }
        }
    }

    private fun calcSubAlpha(mode: GrainAlphaMode, releaseTime: Long): Int =
            when (mode) {
                GrainAlphaMode.DEFAULT -> 0

                GrainAlphaMode.QUESTION -> {
                    val phase = doubleArrayOf(drawAnswerLength * 0.2, drawAnswerLength * 0.7)
                    val timeInQ = timeInQ()

                    when {
                        timeInQ < phase[0] -> (255 * (1.0 - timeInQ.toDouble() / phase[0])).toInt()
                        phase[1].toInt() < timeInQ -> (255.0 * (timeInQ - phase[1]) / (drawAnswerLength - phase[1])).toInt()
                        else -> 0
                    }
                }

                GrainAlphaMode.FADEOUT -> {
                    val tol = 500

                    if (releaseTime > -1L) {
                        val now = System.currentTimeMillis()
                        if (now - releaseTime > tol) {
                            onFadeStart()
                            onFadeStart = {}
                            ((now - releaseTime - tol).toDouble() / 2).toInt()
                        } else {
                            onFadeStart = _onFadeComplete
                            0
                        }
                    } else 0
                }

                GrainAlphaMode.INVISIBLE -> 255
            }

    fun setGrainAlphaMode(mode: GrainAlphaMode) {
        grainAlphaMode = mode
    }

    fun setGrainAlphaModeIntoFadeout(releaseTime: Long, onFadeStart: () -> Unit = {}) {
        grainAlphaMode = GrainAlphaMode.FADEOUT
        this.releaseTime = releaseTime
        _onFadeComplete = onFadeStart
        this.onFadeStart = _onFadeComplete
    }

    fun resetGrainAlphaMode() {
        grainAlphaMode = GrainAlphaMode.DEFAULT
        this.releaseTime = -1L
        onFadeStart = {}
    }

    fun addParticle(x: Float, y: Float, phase: Int? = null) = synchronized(locus) { locus.add(Particle(x, y, canvasHeight, canvasWidth, grainImg, phase)) }

    fun clearParticle() = synchronized(locus) { locus.clear() }

    override fun performClick(): Boolean = super.performClick()
}