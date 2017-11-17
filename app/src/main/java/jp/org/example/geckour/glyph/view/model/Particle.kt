package jp.org.example.geckour.glyph.view.model

import android.graphics.*

class Particle(x: Float, y: Float, val canvasWidth: Int, private val phase: Phase?) {

    enum class Phase {
        NOT_CONVERGING,
        CONVERGING
    }

    companion object {
        private val PI2 = Math.PI * 2

        lateinit var grainImg: Bitmap
        private val grainImgWithAlpha: Bitmap by lazy { grainImg.copy(Bitmap.Config.ARGB_8888, true) }

        fun setGrainAlpha(alpha: Int) {
            if (alpha < 0 || alpha > 255) return

            val w = grainImg.width
            val h = grainImg.height
            val subAlpha = 255 - alpha

            val pixels = IntArray(w * h).apply { grainImg.getPixels(this, 0, w, 0, 0, w, h) }

            pixels.forEachIndexed { i, pixel ->
                val oldAlpha = Color.alpha(pixel)
                val newAlpha = oldAlpha - subAlpha
                pixels[i] =
                        (when {
                            newAlpha < 0 -> 0
                            newAlpha > 255 -> 255
                            else -> newAlpha
                        } shl 24) + (pixel and 0x00ffffff)
            }

            grainImgWithAlpha.setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }
    
    private val grains: ArrayList<Grain> =
            ArrayList<Grain>().apply {
                (0..2).forEach {
                    val master = Grain(x, y)
                    add(master)
                    addAll(List(3) { Grain(x, y, master.start) })
                }
            }
    private val moveUntil: Long = 320
    private var initTime: Long = System.currentTimeMillis()
    private var elapsedTime: Long = 0
    private var o = 0.15
    private val grainR = grainImg.width ushr 1

    private fun phase(): Phase = if (elapsedTime > moveUntil) Phase.CONVERGING else Phase.NOT_CONVERGING

    fun move(canvas: Canvas, paint: Paint) {
        elapsedTime = (System.currentTimeMillis() - initTime)

        when (phase ?: phase()) {
            Phase.NOT_CONVERGING -> { //収束前
                grains.forEach {
                    val param = (moveUntil - elapsedTime).toFloat() / moveUntil
                    it.x = it.end.x + it.distance.x * param
                    it.y = it.end.y + it.distance.y * param
                }
            }
            Phase.CONVERGING -> { //収束後
                if (grains.size > 3) grains.filter { !it.isOrigin }.apply { grains.removeAll(this) }

                grains.forEach {
                    val param = Math.cos(it.paramAngle)
                    it.x = it.end.x + (Math.cos(it.baseAngle) * it.circleR * param).toFloat()
                    it.y = it.end.y + (Math.sin(it.baseAngle) * it.circleR * param).toFloat()
                    it.paramAngle += o
                }
            }
        }
        draw(canvas, paint)
    }

    inner class Grain(var x: Float, var y: Float, basis: PointF? = null) {

        val start: PointF = basis ?: PointF(x, y)
        val end = PointF(x, y)
        val isOrigin = basis == null
        val distance = PointF()
        val baseAngle = PI2 * Math.random()
        var paramAngle = PI2 * Math.random()
        val circleR = canvasWidth * 0.0005 * Math.random() * 4.0 + 5.0

        init {
            val margin = Math.random() * canvasWidth * 0.02
            
            val blurR = canvasWidth * 0.1 * (Math.random() * 0.7 + 0.3) + margin
            val blurA = PI2 * Math.random()

            //収束への出発点
            start.x += (blurR * Math.cos(blurA)).toFloat()
            start.y += (blurR * Math.sin(blurA)).toFloat()

            if (isOrigin) {
                //収束点
                end.x += (margin * Math.cos(blurA)).toFloat()
                end.y += (margin * Math.sin(blurA)).toFloat()
            }

            //収束までの距離
            distance.set(end.x - start.x, end.y - start.y)
        }
    }

    private fun draw(canvas: Canvas, paint: Paint) {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        grains.forEach {
            canvas.drawBitmap(grainImgWithAlpha, it.x - grainR, it.y - grainR, paint)
        }
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }
}