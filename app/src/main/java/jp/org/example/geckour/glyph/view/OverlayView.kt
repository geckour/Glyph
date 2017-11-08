package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.view.model.Particle
import timber.log.Timber
import kotlin.concurrent.thread

class OverlayView: View {
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

    private val paint = Paint()
    private val locus: ArrayList<Particle> = ArrayList()
    private var canvasHeight: Int = 0
    private var canvasWidth: Int = 0
    private val grainImg = BitmapFactory.decodeResource(resources, R.drawable.particle)

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

    private fun drawParticle(canvas: Canvas, paint: Paint) {
        synchronized(locus) {
            for (particle in locus) {
                particle.move(canvas, paint)
            }
        }
    }

    fun addParticle(x: Float, y: Float) = synchronized(locus) { locus.add(Particle(x, y, canvasHeight, canvasWidth, grainImg)) }

    fun clearParticle() = synchronized(locus) { locus.clear() }

    override fun performClick(): Boolean = super.performClick()
}