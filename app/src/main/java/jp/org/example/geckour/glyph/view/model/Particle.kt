package jp.org.example.geckour.glyph.view.model

import android.graphics.*
import timber.log.Timber

class Particle(x: Float, y: Float, canvasHeight: Int, val canvasWidth: Int, grainImg: Bitmap) {
    
    private val grains: ArrayList<Grain> = ArrayList(List(3) { Grain(x, y) })
            .apply {
                (0..2).forEach {
                    addAll(List(3) { Grain(x, y, this[it].start) })
                }
            }
    private val moveUntil: Long = 320
    private var initTime: Long = System.currentTimeMillis()
    private var elapsedTime: Long = 0
    private var v = 0.15
    private var grainR = 16f * canvasHeight / 1280

    private var scaledGrain = Bitmap.createScaledBitmap(grainImg, (grainR * 2).toInt(), (grainR * 2).toInt(), false)

    private fun phase(): Int = if (elapsedTime > moveUntil) 1 else 0

    fun move(canvas: Canvas, paint: Paint) {
        elapsedTime = (System.currentTimeMillis() - initTime).apply { Timber.d("elapsedTime: $this") }

        when (phase()) {
            0 -> { //収束前
                grains.forEach {
                    val param = (moveUntil - elapsedTime).toFloat() / moveUntil
                    it.x = it.end.x + it.diff.x * param
                    it.y = it.end.y + it.diff.y * param
                }
            }
            1 -> { //収束後
                grains.filterNot { it.isOrigin }.apply { grains.removeAll(this) }

                grains.forEach {
                    val param = Math.cos(it.paramAngle)
                    it.x += (Math.cos(it.baseAngle) * it.circleR * param).toFloat()
                    it.y += (Math.sin(it.baseAngle) * it.circleR * param).toFloat()
                    it.paramAngle += v
                }
            }
        }
        draw(canvas, paint)
    }

    inner class Grain(var x: Float, var y: Float, val start: PointF = PointF(-1f, -1f)) {
        val end = PointF()
        val isOrigin = start.x < 0 || start.y < 0
        val diff = PointF()
        private val pi2 = Math.PI * 2.0
        var paramAngle = Math.random() * pi2
        val baseAngle = Math.random() * pi2
        val circleR = Math.random() * 0.5 + 0.7

        init {
            val margin = Math.random() * canvasWidth * 0.03
            
            var blurR: Double
            var blurA: Double = Math.random() * pi2
            if (isOrigin) {
                //収束への出発点
                blurR = canvasWidth * 0.4 * Math.random() + margin
                start.x = x + (blurR * Math.cos(blurA)).toFloat()
                start.y = y + (blurR * Math.sin(blurA)).toFloat()

                //収束点
                blurR = margin
                blurA = Math.random() * pi2
                end.x = x + (blurR * Math.cos(blurA)).toFloat()
                end.y = y + (blurR * Math.sin(blurA)).toFloat()
            } else {
                //収束への出発点
                blurR = canvasWidth * 0.2 * Math.random()
                start.x += (blurR * Math.cos(blurA)).toFloat()
                start.y += (blurR * Math.sin(blurA)).toFloat()

                //収束点
                end.set(x, y)
            }

            //収束までの距離
            diff.x = end.x - start.x
            diff.y = end.y - start.y
        }
    }

    private fun draw(canvas: Canvas, paint: Paint) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        grains.filter { it.isOrigin }.forEach {
            canvas.drawBitmap(scaledGrain, it.x - grainR, it.y - grainR, paint)
        }
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }
}