package jp.org.example.geckour.glyph.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import jp.org.example.geckour.glyph.*
import jp.org.example.geckour.glyph.App.Companion.version
import jp.org.example.geckour.glyph.util.*
import kotlinx.coroutines.experimental.Job
import kotlin.collections.ArrayList

class DotsView: View {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int): super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context): super(context)

    private val jobList: ArrayList<Job> = ArrayList()

    private val paint = Paint()
    private var offsetWidth = 0f
    private var offsetHeight = 0f
    private var radius = 0f
    private var dotDiam = 0
    private val dotBitmapTrue: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.dot_t).let {
            Bitmap.createScaledBitmap(it, dotDiam, dotDiam, false)
        }
    }
    private val dotBitmapFalse: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.dot_f).let {
            Bitmap.createScaledBitmap(it, dotDiam, dotDiam, false)
        }
    }
    private val dots = Array(11) { PointF() }
    private val isThrough = Array(11) { false }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        offsetWidth = (right - left) * 0.5f
        offsetHeight = (bottom - top) * 0.5f

        radius = offsetWidth * 0.45f
        dotDiam = (offsetWidth * 0.15).toInt()

        val uAngle = Math.PI / 3.0

        dots.forEachIndexed { i, pointF ->
            val c = when (i) {
                1 -> 1
                2 -> 3
                3 -> 4
                4 -> 6
                in 5..10 -> i
                else -> 0
            }

            if (i == 0) pointF.set(offsetWidth, offsetHeight * 1.2f)
            else {
                pointF.set(
                        Math.cos(uAngle * (c - 0.5)).toFloat() * (if (i < 5) radius else radius * 2f) + offsetWidth,
                        Math.sin(uAngle * (c - 0.5)).toFloat() * (if (i < 5) radius else radius * 2f) + offsetHeight * 1.2f
                )
            }
        }

        paint.isAntiAlias = true

        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        jobList.clearJobs()
    }

    override fun onDraw(canvas: Canvas) {
        isThrough.forEachIndexed { i, b ->
            if (b) {
                canvas.drawBitmap(dotBitmapTrue, dots[i].x - dotDiam / 2, dots[i].y - dotDiam / 2, paint)
            } else {
                canvas.drawBitmap(dotBitmapFalse, dots[i].x - dotDiam / 2, dots[i].y - dotDiam / 2, paint)
            }
        }
    }

    fun setDotState(index: Int, b: Boolean) {
        if (index in 0..10) isThrough[index] = b
        invalidate()
    }

    fun setDotsState(predicate: (Int) -> Boolean) {
        isThrough.forEachIndexed { i, _ ->
            isThrough[i] = predicate(i)
        }
        invalidate()
    }

    fun setDotsState(indices: List<Pair<Int, Boolean>>) {
        indices.forEach {
            if (it.first in 0..10) {
                isThrough[it.first] = it.second
            }
        }
        invalidate()
    }

    fun getDots(): Array<PointF> = dots

    fun getCollision(fromX: Float, fromY: Float, toX: Float, toY: Float, onCollision: (List<Int>) -> Unit = {}): List<Int> {
        val collisionDots: ArrayList<Int> = ArrayList()
        val tol = dotDiam
        for (i in 0..10) {
            if (fromX == toX && fromY == toY) {
                //円の方程式にて当たり判定
                val diffX = fromX - dots[i].x
                val diffY = fromY - dots[i].y

                if (diffX * diffX + diffY * diffY < tol * tol) {
                    isThrough[i] = true
                    collisionDots.add(i)
                }
            } else {
                //線分と円の当たり判定
                val a = fromY - toY
                val b = toX - fromX
                val c = fromX * toY - toX * fromY
                val d = (a * dots[i].x + b * dots[i].y + c) / Math.sqrt((a * a + b * b).toDouble())

                if (-tol <= d && d <= tol) {
                    //線分への垂線と半径
                    val diffFromX = fromX - dots[i].x
                    val diffToX = toX - dots[i].x
                    val diffFromY = fromY - dots[i].y
                    val diffToY = toY - dots[i].y
                    val difXA = toX - fromX
                    val difYB = toY - fromY
                    val innerA = (diffFromX * difXA + diffFromY * difYB).toDouble()
                    val innerB = (diffToX * difXA + diffToY * difYB).toDouble()
                    val dA = Math.sqrt((diffFromX * diffFromX + diffFromY * diffFromY).toDouble())
                    val dB = Math.sqrt((diffToX * diffToX + diffToY * diffToY).toDouble())
                    if (innerA * innerB <= 0) {
                        //内積
                        isThrough[i] = true
                        collisionDots.add(i)
                    } else if (dA < tol || dB < tol) {
                        isThrough[i] = true
                        collisionDots.add(i)
                    }
                }
            }
        }

        return collisionDots.distinct().apply { if (this.isNotEmpty()) onCollision(this) }
    }
}