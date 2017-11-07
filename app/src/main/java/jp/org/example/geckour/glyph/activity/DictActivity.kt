package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityMainBinding

class DictActivity : Activity() {

    companion object {
        fun createIntent(activity: Activity): Intent =
                Intent(activity, DictActivity::class.java)
    }

    private val tag = this::class.java.simpleName
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val t: Tracker? = (application as App).getTracker(App.TrackerName.APP_TRACKER)
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }
/*
    internal inner class DictView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
        var thread: Thread? = null
        var canvas: Canvas? = null
        val paint: Paint = Paint()
        var typeface: Typeface? = null
        var dbHelper: DBHelper
        var db: SQLiteDatabase
        val grainImg: Bitmap
        var scaledGrain: Bitmap? = null
        var dotTrue: Bitmap? = null
        var dotFalse: Bitmap? = null

        var isAttached: Boolean = false
        var cr = Math.PI / 3
        var radius: Double = 0.toDouble()
        var dotDiam: Int = 0
        var grainR: Float = 0f
        var isThrough = BooleanArray(11)
        var initTime: Long = 0
        var pressButtonTime: Long = 0
        var doVibrate = false
        var isPressedButton = false
        var resultId = ArrayList<Int>()

        var dots = arrayOfNulls<PointF>(11)
        var Locus = ArrayList<Particle>()
        var locusPath = Path()
        var now: Long = 0
        var throughList: ThroughList = ThroughList()
        var holdTime: Long = 0
        var previousDot = -1

        init {
            holder.addCallback(this)
            dbHelper = DBHelper(context)
            db = dbHelper.readableDatabase

            radius = offsetX * 0.8
            dotDiam = (radius / 4.5).toInt()
            grainR = 20 * scale
            dotTrue = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.dot_t), dotDiam, dotDiam, false)
            dotFalse = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.dot_f), dotDiam, dotDiam, false)
            dots[0] = PointF(offsetX, offsetY * 1.2f)
            for (i in 1..4) {
                var j = i
                if (i > 1) {
                    j++
                    if (i > 3) {
                        j++
                    }
                }
                dots[i] = PointF((Math.cos(cr * (j - 0.5)) * (radius / 2) + offsetX).toFloat(), (Math.sin(cr * (j - 0.5)) * (radius / 2) + offsetY * 1.2).toFloat())
            }
            for (i in 5..10) {
                dots[i] = PointF((Math.cos(cr * (i - 0.5)) * radius + offsetX).toFloat(), (Math.sin(cr * (i - 0.5)) * radius + offsetY * 1.2).toFloat())
            }

            doVibrate = sp?.getBoolean("doVibrate", false) ?: false
            for (i in 0..isThrough.lastIndex) {
                isThrough[i] = false
            }

            grainImg = BitmapFactory.decodeResource(resources, R.drawable.particle)

            now = System.currentTimeMillis()
            initTime = now
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            isAttached = true
            thread = Thread(this)
            thread?.start()
        }
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            isAttached = false
        }

        override fun run() {
            val tag = "DictView/run"
            while (isAttached) {
                draw()
                try {
                    Thread.sleep(10)
                } catch (e: Exception) {
                    Log.e(tag, e.message)
                }
            }
        }

        private fun draw() {
            val tag = "DictView/draw"
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                this.canvas = canvas
                onDraw(canvas)
            } catch (e: IllegalStateException) {
                Log.e(tag, e.message)
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        public override fun onDraw(canvas: Canvas) {
            canvas.drawColor(if (version >= 23) resources.getColor(R.color.background, null) else resources.getColor(R.color.background))
            paint.isAntiAlias = true
            typeface = Typeface.createFromAsset(context.assets, "coda_regular.ttf")
            //paint.setTypeface(typeface)
            paint.typeface = typeface

            setGrainAlpha(releaseTime)

            for (i in 0..10) {
                if (isThrough[i]) {
                    canvas.drawBitmap(dotTrue, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, paint)
                } else {
                    canvas.drawBitmap(dotFalse, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, paint)
                }
            }
            synchronized(Locus) {
                for (particle in Locus) {
                    particle.move()
                }
            }
            if (isReleased) {
                drawResult(canvas)
            }
            //drawFPS();

            now = if (isPressedButton) System.currentTimeMillis() - pressButtonTime + holdTime else System.currentTimeMillis()
        }

        internal inner class ThroughList {
            var dots: ArrayList<Int>

            constructor() {
                dots = ArrayList()
            }

            constructor(argDots: ArrayList<Int>) {
                dots = ArrayList(argDots)
            }

            constructor(argDots: Array<String>) {
                val tag = "ThroughList"
                dots = ArrayList()
                for (s in argDots) {
                    try {
                        dots.add(Integer.parseInt(s))
                    } catch (e: Exception) {
                        Log.e(tag, e.message)
                    }

                }
            }
        }

        private fun setGrainAlpha(time: Long) {
            scaledGrain = Bitmap.createScaledBitmap(grainImg, (grainR * 2).toInt(), (grainR * 2).toInt(), false)
            val w = scaledGrain?.width ?: 0
            val h = scaledGrain?.height ?: 0

            val pixels = IntArray(w * h)
            scaledGrain?.getPixels(pixels, 0, w, 0, 0, w, h)

            var subAlpha = 0
            if (time > -1) {
                subAlpha = calcSubAlpha(time)
            }
            if (subAlpha != 0) {
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        var a = pixels[x + y * w]
                        var b = a
                        a = a.ushr(24)

                        if (a != 0) {
                            a -= subAlpha
                            if (a < 0) {
                                a = 0
                            }
                        }
                        a = a shl 24

                        b = b and 16777215

                        pixels[x + y * w] = a + b
                    }
                }
                scaledGrain?.setPixels(pixels, 0, w, 0, 0, w, h)
            }
        }

        private fun calcSubAlpha(time: Long): Int {
            val tol = 500
            return if (now - time > tol) {
                resetThrough()
                var result = ((now - time - tol.toLong()) / 2f).toInt()
                if (result > 255) {
                    result = 255
                }
                result
            } else {
                0
            }
        }

        internal inner class Particle(var x0: Float, var y0: Float, val canvas: Canvas) {
            var grain = ArrayList<Grain>()
            var phase = 0
            var moveFrames: Long = 300
            var initFrame: Long = 0
            var v = 0.15

            init {
                initFrame = System.currentTimeMillis()
                grain.add(Grain(x0, y0, true, null))
                grain.add(Grain(x0, y0, true, null))
                grain.add(Grain(x0, y0, true, null))
                grain.add(Grain(grain[0].origin.x, grain[0].origin.y, false, grain[0].step0))
                grain.add(Grain(grain[0].origin.x, grain[0].origin.y, false, grain[0].step0))
                grain.add(Grain(grain[0].origin.x, grain[0].origin.y, false, grain[0].step0))
                grain.add(Grain(grain[1].origin.x, grain[1].origin.y, false, grain[1].step0))
                grain.add(Grain(grain[1].origin.x, grain[1].origin.y, false, grain[1].step0))
                grain.add(Grain(grain[1].origin.x, grain[1].origin.y, false, grain[1].step0))
                grain.add(Grain(grain[2].origin.x, grain[2].origin.y, false, grain[2].step0))
                grain.add(Grain(grain[2].origin.x, grain[2].origin.y, false, grain[2].step0))
                grain.add(Grain(grain[2].origin.x, grain[2].origin.y, false, grain[2].step0))
            }

            fun move() {
                val diffFrames = System.currentTimeMillis() - initFrame
                if (diffFrames > moveFrames || isReleased) phase = 1

                when (phase) {
                    0 -> {
                        val param = (moveFrames - diffFrames) / (moveFrames.toFloat())
                        for (i in grain.indices) {
                            grain[i].x = grain[i].step1.x + grain[i].diff.x * param
                            grain[i].y = grain[i].step1.y + grain[i].diff.y * param
                        }
                    }
                    1 -> {
                        (grain.lastIndex downTo 0)
                                .filterNot { grain[it].isOrigin }
                                .forEach { grain.removeAt(it) }
                        for (i in grain.indices) {
                            val param = Math.cos(grain[i].a0)
                            grain[i].x += (Math.cos(grain[i].a1) * grain[i].circleR * param).toFloat()
                            grain[i].y += (Math.sin(grain[i].a1) * grain[i].circleR * param).toFloat()
                            grain[i].a0 += v
                        }
                    }
                }
                draw()
            }

            internal inner class Grain(x: Float, y: Float, isOrigin: Boolean, start: PointF?) {
                var x: Float
                var y: Float
                val isOrigin = isOrigin
                var origin = PointF()
                var step0 = PointF()
                var step1 = PointF()
                var diff = PointF()
                val pi2 = Math.PI * 2.0
                var a0 = Math.random() * pi2
                val a1 = Math.random() * pi2
                var circleR = Math.random() * 0.5 + 0.7

                init {
                    //タッチした点
                    origin.x = x
                    origin.y = y

                    val margin = Math.random() * offsetX * 0.05

                    //収束への出発点
                    var blurR: Double
                    var blurA: Double
                    blurA = Math.random() * pi2
                    if (isOrigin) {
                        blurR = offsetX * 0.4 * Math.random() + margin
                        step0.x = origin.x + (blurR * Math.cos(blurA)).toFloat()
                        step0.y = origin.y + (blurR * Math.sin(blurA)).toFloat()
                    } else if (start != null) {
                        blurR = offsetX * 0.2 * Math.random()
                        step0.x = start.x + (blurR * Math.cos(blurA)).toFloat()
                        step0.y = start.y + (blurR * Math.sin(blurA)).toFloat()
                    }

                    //収束点
                    if (isOrigin) {
                        blurR = margin
                        blurA = Math.random() * pi2
                        step1.x = origin.x + (blurR * Math.cos(blurA)).toFloat()
                        step1.y = origin.y + (blurR * Math.sin(blurA)).toFloat()
                    } else  {
                        step1.x = x
                        step1.y = y
                    }

                    //収束までの距離
                    diff.x = step0.x - step1.x
                    diff.y = step0.y - step1.y

                    if (isReleased) {
                        this.x = step1.x
                        this.y = step1.y
                    } else {
                        this.x = step0.x
                        this.y = step0.y
                    }
                }
            }

            private fun draw() {
                //paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.ADD))
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                for (gr in grain) {
                    canvas.drawBitmap(scaledGrain, gr.x - grainR, gr.y - grainR, paint)
                }
                //paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_OVER))
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
        }



        private fun putParticles(throughList: ThroughList, canvas: Canvas) {
            val interval = 25 * scale
            val length = FloatArray(throughList.dots.lastIndex)
            for (i in 1..throughList.dots.lastIndex) {
                val dotI0 = dots[throughList.dots[i]]
                val dotI1 = dots[throughList.dots[i - 1]]
                if (dotI0 != null && dotI1 != null) {
                    length[i - 1] = Math.sqrt(Math.pow((dotI1.x - dotI0.x).toDouble(), 2.0) + Math.pow((dotI1.y - dotI0.y).toDouble(), 2.0)).toFloat()
                }
            }
            synchronized(Locus) {
                Locus.clear()
            }
            for (i in length.indices) {
                val dotI0 = dots[throughList.dots[i]]
                val dotI1 = dots[throughList.dots[i + 1]]
                if (dotI0 != null && dotI1 != null) {
                    val unitV = floatArrayOf((dotI1.x - dotI0.x) / length[i], (dotI1.y - dotI0.y) / length[i])
                    var x = dotI0.x
                    var y = dotI0.y

                    val sumLength = floatArrayOf(0f, 0f)
                    synchronized(Locus) {
                        val absX = Math.abs(dotI1.x - dotI0.x)
                        val absY = Math.abs(dotI1.y - dotI0.y)
                        val dX = unitV[0] * interval
                        val dY = unitV[1] * interval
                        val dXa = absX * interval / length[i]
                        val dYa = absY * interval / length[i]
                        while (sumLength[0] <= absX && sumLength[1] <= absY) {
                            Locus.add(Particle(x, y, canvas))
                            x += dX
                            y += dY
                            sumLength[0] += dXa
                            sumLength[1] += dYa
                        }
                    }
                }
            }
        }

        private fun searchIdFromDB(): ArrayList<Int> {
            val searchResult = ArrayList<Int>()
            val cursor: Cursor = db.query(DBHelper.TABLE_NAME1, arrayOf("id", "path"), null, null, null, null, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val dotsSplit = cursor.getString(cursor.getColumnIndex("path")).split(",".toRegex()).toTypedArray()
                val throughListInRow = ThroughList(dotsSplit)
                if (judgeLocus(throughListInRow, throughList)) {
                    searchResult.add(cursor.getInt(cursor.getColumnIndex("id")))
                }
                cursor.moveToNext()
            }
            cursor.close()
            return searchResult
        }

        private fun drawResult(canvas: Canvas) {
            val resultStr = ArrayList<String>()
            synchronized(resultId) {
                val resultIdInString = Array(resultId.size, { i -> "" })
                for (i in 0..resultId.lastIndex) {
                    resultIdInString[i] = resultId[i].toString()
                    val cursor = db.query(DBHelper.TABLE_NAME1, null, "id = ?", arrayOf(resultIdInString[i]), null, null, null)
                    cursor.moveToFirst()
                    resultStr.add(cursor.getString(cursor.getColumnIndex("name")))
                    cursor.close()
                }
            }

            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 1f
            paint.textSize = 70 * scale
            paint.textAlign = Paint.Align.CENTER
            for (i in 0..resultStr.lastIndex) canvas.drawText(resultStr[i], offsetX, (offsetY * 1.2f - offsetX) / 2f + i * paint.textSize * 1.2f, paint)
        }

        private fun normalizePaths(paths: ArrayList<IntArray>): ArrayList<IntArray> {
            val returnPaths = ArrayList<IntArray>()
            for (i in paths.indices) {
                var match = 0
                val srcPath = paths[i]
                for (j in i + 1 until paths.size) {
                    val destPath = paths[j]
                    val tempPath = intArrayOf(destPath[1], destPath[0])
                    if (Arrays.equals(srcPath, destPath) || Arrays.equals(srcPath, tempPath)) {
                        match++
                    }
                }
                if (match == 0) {
                    returnPaths.add(srcPath)
                }
            }

            return returnPaths
        }

        private fun judgeLocus(answer: ThroughList, through: ThroughList): Boolean {

            val answerPaths =
                    (0 until answer.dots.size - 1).map { intArrayOf(answer.dots[it], answer.dots[it + 1]) }
            var passedPaths =
                    (0 until through.dots.size - 1).mapTo(ArrayList()) { intArrayOf(through.dots[it], through.dots[it + 1]) }
            passedPaths = normalizePaths(passedPaths)

            return if (answerPaths.size == passedPaths.size) {
                val clearFrags = BooleanArray(answerPaths.size)
                for (i in answerPaths.indices) {
                    for (path in passedPaths) {
                        val tempPaths = intArrayOf(path[1], path[0])
                        if (Arrays.equals(answerPaths[i], path) || Arrays.equals(answerPaths[i], tempPaths)) {
                            clearFrags[i] = true
                        }
                    }
                }
                val clearC = clearFrags.count { it }
                (clearC == answerPaths.size)
            } else {
                false
            }
        }

        private fun setLocusStart(x: Float, y: Float, doCD: Boolean, canvas: Canvas) {
            synchronized(Locus) {
                Locus.add(Particle(x, y, canvas))

                if (doCD) {
                    setCollision(x, y, x, y)
                }
                locusPath.moveTo(x, y)
            }
        }

        private fun setLocus(x: Float, y: Float, doCD: Boolean, canvas: Canvas) {
            synchronized(Locus) {
                Locus.add(Particle(x, y, canvas))

                if (doCD) {
                    setCollision(x, y, Locus[Locus.size - 2].x0, Locus[Locus.size - 2].y0)
                }
                locusPath.lineTo(x, y)
            }
        }

        private fun setCollision(x0: Float, y0: Float, x1: Float, y1: Float) {
            var collisionDot = -1
            val tol = 35 * scale
            for (i in 0..10) {
                if (x0 == x1 && y0 == y1) {
                    //円の方程式にて当たり判定
                    val difX = x0 - dots[i]!!.x
                    val difY = y0 - dots[i]!!.y
                    val r = offsetX * 0.8 / 18 + tol
                    if (difX * difX + difY * difY < r * r) {
                        isThrough[i] = true
                        collisionDot = i
                    }
                } else {
                    //線分と円の当たり判定
                    val a = y0 - y1
                    val b = x1 - x0
                    val c = x0 * y1 - x1 * y0
                    val d = (a * dots[i]!!.x + b * dots[i]!!.y + c) / Math.sqrt((a * a + b * b).toDouble())
                    val lim = offsetX * 0.8 / 18 + tol
                    if (-lim <= d && d <= lim) {
                        //線分への垂線と半径
                        val difX0 = dots[i]!!.x - x0
                        val difX1 = dots[i]!!.x - x1
                        val difY0 = dots[i]!!.y - y0
                        val difY1 = dots[i]!!.y - y1
                        val difX10 = x1 - x0
                        val difY10 = y1 - y0
                        val inner0 = (difX0 * difX10 + difY0 * difY10).toDouble()
                        val inner1 = (difX1 * difX10 + difY1 * difY10).toDouble()
                        val d0 = Math.sqrt((difX0 * difX0 + difY0 * difY0).toDouble())
                        val d1 = Math.sqrt((difX1 * difX1 + difY1 * difY1).toDouble())
                        if (inner0 * inner1 <= 0) {
                            //内積
                            isThrough[i] = true
                            collisionDot = i
                        } else if (d0 < lim || d1 < lim) {
                            isThrough[i] = true
                            collisionDot = i
                        }
                    }
                }
            }
            if (collisionDot != -1 && (throughList.dots.size < 1 || throughList.dots[throughList.dots.size - 1] != collisionDot)) {
                throughList.dots.add(collisionDot)
                if (doVibrate) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(30)
                }
                previousDot = collisionDot
            }
        }

        private fun resetLocus() {
            locusPath.reset()
            synchronized(Locus) {
                Locus.clear()
            }
        }

        private fun resetThrough() {
            for (i in 0..10) {
                isThrough[i] = false
            }
        }

        var downX = 0f
        var downY = 0f
        var memX = 0f
        var memY = 0f
        var isReleased = false
        var releaseTime: Long = -1
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val tag = "onTouchEvent"

            val lim = 15 * scale
            when (event.action) {
                MotionEvent.ACTION_DOWN //タッチ
                -> {
                    downX = event.x
                    downY = event.y
                    if (isReleased) {
                        isReleased = false
                        resetLocus()
                        resetThrough()
                        throughList.dots.clear()
                        resultId.clear()
                    }
                    setLocusStart(downX, downY, true, canvas!!)
                    memX = downX
                    memY = downY
                }
                MotionEvent.ACTION_MOVE //スワイプ
                -> {
                    val currentX = event.x
                    val currentY = event.y
                    if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                        if (Locus.size == 0) {
                            setLocusStart(currentX, currentY, true, canvas!!)
                        } else {
                            setLocus(currentX, currentY, true, canvas!!)
                        }
                        memX = currentX
                        memY = currentY
                    }
                }
                MotionEvent.ACTION_UP //リリース
                -> {
                    isReleased = true
                    var list = ""
                    for (throughDot in throughList.dots) {
                        list += "$throughDot,"
                    }
                    Log.v(tag, "throughList:$list")
                    resetLocus()
                    if (throughList.dots.size > 0) {
                        putParticles(throughList, canvas!!)
                    }
                    synchronized(resultId) {
                        resultId = searchIdFromDB()
                    }
                }
                MotionEvent.ACTION_CANCEL
                -> {}
            }
            return true
        }
    }
*/
}