package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ActivityMainBinding
import jp.org.example.geckour.glyph.fragment.CheckAnswerFragment
import jp.org.example.geckour.glyph.fragment.MainFragment
import jp.org.example.geckour.glyph.fragment.model.Result

class MainActivity : AppCompatActivity() {

    enum class Mode { // TODO: Modeのハンドル
        NORMAL,
        WEAKNESS
    }

    companion object {
        val tag = this::class.java.simpleName

        fun createIntent(activity: Activity, mode: Mode): Intent =
                Intent(activity, MainActivity::class.java).apply {
                    putExtra(ARGS_MODE, mode)
                }

        private val ARGS_MODE = "mode"

        var hacks: Int = 0
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mode: Mode

    internal var level: Int? = null
    internal var sequenceId: Long? = null

    internal val scale: Float by lazy { binding.container.height.toFloat() / 1280 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBar?.hide()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        hacks++

        val fragment = MainFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, MainFragment.tag)
                .commit()

        if (intent.hasExtra(ARGS_MODE)) mode = intent.getSerializableExtra(ARGS_MODE) as Mode

        val t: Tracker? = (application as App).getTracker(App.TrackerName.APP_TRACKER)
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onBackPressed() {
        finish()
    }

    internal fun transitionForheckAnswer(results: List<Result>, allowableTime: Long) {
        val fragment = CheckAnswerFragment.newInstance(results, allowableTime)
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, CheckAnswerFragment.tag)
                .addToBackStack(CheckAnswerFragment.tag)
                .commit()
    }

    internal fun getMode(): Mode = mode

    internal fun onRetry() = supportFragmentManager.popBackStack()

    internal fun onNext() = startActivity(MainActivity.createIntent(this, mode))
/*
    internal inner class MyView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
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
        var state = true
        var gameMode: Int = 0
        var level: Int = 0
        var randomVal: Int = 0
        var cr = Math.PI / 3
        var radius: Double = 0.toDouble()
        var dotDiam: Int = 0
        var grainR: Float = 0f
        var isThrough = Array(11, { i -> false })
        var qTotal = 0
        var qNum = 0
        var defTime = 20000
        var initTime: Long = 0
        var drawAnswerLength = 1200
        var marginTime: Long = 900
        var pressButtonTime: Long = 0
        var isFirstTimeUp = true
        var doVibrate = false
        var doDrawCount = false
        var isCmdSeq = true
        var isStartGame = false
        var isEndGame = false
        var doShow = true
        var isPressedButton = false

        var dots = arrayOfNulls<PointF>(11)
        var locus = ArrayList<Particle>()
        var locusPath = Path()
        var now: Long = 0
        var throughDots: Array<ThroughList?>
        var answerThroughList: Array<ThroughList?>
        var difficulty = ArrayList<Difficulty>()
        var correctStr = ArrayList<String>()
        var holdTime: Long = 0
        var nextButtonPoint = arrayOfNulls<Point>(2)
        var redoButtonPoint = arrayOfNulls<Point>(2)
        var retryButtonPoint = arrayOfNulls<Point>(2)
        var previousDot = -1
        var passTime: Array<Long>

        init {
            holder.addCallback(this)
            val tag = "MyView/init"
            dbHelper = DBHelper(context)
            db = dbHelper.readableDatabase

            radius = offsetX * 0.8
            dotDiam = (radius / 4.5).toInt()
            grainR = 20 * scale
            dotTrue = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.dot_t), dotDiam, dotDiam, false)
            dotFalse = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources, R.drawable.dot_f), dotDiam, dotDiam, false)
            dots[0] = PointF(offsetX, (offsetY * 1.2).toFloat())
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

            var giveTime = 20000
            var giveQs = 1
            for (i in 0..8) {
                if (i > 3) {
                    giveTime -= 1000
                }
                if (i == 2 || i == 3 || i == 6 || i == 8) {
                    giveQs++
                }
                difficulty.add(i, Difficulty(giveQs, giveTime))
            }
            gameMode = Integer.parseInt(sp?.getString("gamemode", "0") ?: "0")
            doVibrate = sp?.getBoolean("doVibrate", false) ?: false
            doDrawCount = sp?.getBoolean("showCountView", false) ?: false
            level = if (receivedLevel > -1) receivedLevel else (Math.random() * (max - min + 1) + min).toInt()
            //level = 2
            qTotal = difficulty[level].qs
            passTime = Array(qTotal, { i -> -1L })
            Log.d(tag, "qTotal:" + qTotal)
            defTime = difficulty[level].time

            throughDots = arrayOfNulls(qTotal)
            answerThroughList = arrayOfNulls(qTotal)
            getSequence()

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
            val tag = "MyView/run"
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
            val tag = "MyView/draw"
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

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(if (version >= 23) resources.getColor(R.color.background, null) else resources.getColor(R.color.background))
            paint.isAntiAlias = true
            typeface = Typeface.createFromAsset(context.assets, "coda_regular.ttf")
            //paint.setTypeface(typeface)
            paint.typeface = typeface

            if (doDrawCount) {
                drawCount(canvas)
            }

            if (doShow) {
                for (i in 0..10) {
                    if (isThrough[i]) {
                        canvas.drawBitmap(dotTrue, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, paint)
                    } else {
                        canvas.drawBitmap(dotFalse, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, paint)
                    }
                }
                synchronized(locus) {
                    for (particle in locus) {
                        particle.move(phase(particle))
                    }
                }
            }

            if (isCmdSeq) {
                setGrainAlpha(releaseTime)
                listenCmd(canvas)
            } else {
                if (!isStartGame) {
                    drawAnswer(initTime, now, canvas)
                    //paint.color = Color.WHITE
                } else if (!isEndGame) {
                    setGrainAlpha(releaseTime)
                }

                if (isStartGame && doShow) {
                    drawTime(now, canvas)
                    drawQueNumber(now - initTime, 0, 2, 255, 197, canvas)
                } else if (!isStartGame) {
                    drawQueNumber(now - initTime, marginTime, 240, 150, 40, canvas)
                }

                drawButton(canvas)
                //drawFPS()
            }

            if (isEndGame) {
                if (now > holdTime + marginTime) {
                    doShowRedo = false
                    if (!isRecorded) recordResult()

                    doShow = false
                    drawResult(marginTime, holdTime + marginTime, now, canvas)
                }
            }

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

        internal inner class Difficulty(argQs: Int, argTime: Int) {
            var qs = 0
            var time = 0

            init {
                qs = argQs
                time = argTime
            }
        }

        private fun getSequence() {
            val tag = "MyView/getSequence"
            val l = 12

            if (qTotal > 1) {
                val shapesSplit: Array<String>
                val cursor = db.query(DBHelper.TABLE_NAME2, null, null, null, null, null, null)

                if (isWeaknessMode) {
                    val cursorForWeakness = db.query(DBHelper.TABLE_NAME2, null, "level = $qTotal and total_number > 0", null, null, null, "cast(correct_number as double) / total_number asc", "$l")

                    val n = cursorForWeakness.count
                    Log.d(tag, "n: $n")
                    if (n > 0) {
                        val r = (Math.random() * n).toInt()
                        cursorForWeakness.moveToPosition(r)
                        randomVal = cursorForWeakness.getInt(cursorForWeakness.getColumnIndex("id")) - 1
                    } else {
                        val cursorInLevel = db.query(DBHelper.TABLE_NAME2, null, "level = $qTotal", null, null, null, null)

                        cursorInLevel.moveToFirst()
                        val min = cursorInLevel.getLong(0)
                        cursorInLevel.moveToLast()
                        val max = cursorInLevel.getLong(0)
                        randomVal = if (receivedValue > -1) receivedValue else (Math.random() * (max - min) + min).toInt()
                        //randomVal = 0

                        cursorInLevel.close()
                    }

                    cursorForWeakness.close()
                } else {
                    val cursorInLevel = db.query(DBHelper.TABLE_NAME2, null, "level = $qTotal", null, null, null, null)

                    cursorInLevel.moveToFirst()
                    val min = cursorInLevel.getLong(0)
                    cursorInLevel.moveToLast()
                    val max = cursorInLevel.getLong(0)
                    randomVal = if (receivedValue > -1) receivedValue else (Math.random() * (max - min) + min).toInt()
                    //randomVal = 633 - 171

                    cursorInLevel.close()
                }
                cursor.moveToPosition(randomVal)
                shapesSplit = cursor.getString(2).split(",".toRegex()).toTypedArray()
                Log.d(tag, "randomVal:$randomVal, level:$level")
                for (s in shapesSplit) {
                    Log.d(tag, "shapesSplit: " + s)
                }
                correctStr = getCorrectStrings(cursor)

                cursor.close()

                for (i in 0 until qTotal) {
                    throughDots[i] = ThroughList()
                    val cursorInName = db.rawQuery("select * from ${DBHelper.TABLE_NAME1} where name = '${shapesSplit[i].replace("'", "''")}';", null)
                    cursorInName.moveToFirst()
                    //Log.d(tag, "shaper name: " + c.getString(1));
                    val dotsSplit = cursorInName.getString(cursorInName.getColumnIndex("path")).split(",".toRegex()).toTypedArray()
                    cursorInName.close()
                    answerThroughList[i] = ThroughList(dotsSplit)
                }
            } else {
                val cursor = db.query(DBHelper.TABLE_NAME1, null, null, null, null, null, null)

                if (isWeaknessMode) {
                    val cursorForWeakness = db.query(DBHelper.TABLE_NAME1, null, "total_number > 0", null, null, null, "cast(correct_number as double) / total_number asc", "$l")

                    val n = cursorForWeakness.count
                    Log.d(tag, "n: $n")
                    if (n > 0) {
                        val r = (Math.random() * n).toInt()
                        cursorForWeakness.moveToPosition(r)
                        randomVal = cursorForWeakness.getInt(cursorForWeakness.getColumnIndex("id")) - 1
                    } else {
                        cursor.moveToLast()
                        val max = cursor.getLong(0)
                        randomVal = if (receivedValue > -1) receivedValue else (Math.random() * max).toInt()
                        //int randomVal = (int)max - 1;
                        Log.d(tag, "randomVal:$randomVal, level:$level")
                        throughDots[0] = ThroughList()
                    }

                    cursorForWeakness.close()
                } else {
                    cursor.moveToLast()
                    val max = cursor.getLong(0)
                    randomVal = if (receivedValue > -1) receivedValue else (Math.random() * max).toInt()
                    //randomVal = 0
                    Log.d(tag, "randomVal:$randomVal, level:$level")
                    throughDots[0] = ThroughList()
                }
                cursor.moveToPosition(randomVal)
                val dotsSplit = cursor.getString(cursor.getColumnIndex("path")).split(",".toRegex()).toTypedArray()
                answerThroughList[0] = ThroughList(dotsSplit)
                throughDots[0] = ThroughList()
                correctStr.add(cursor.getString(cursor.getColumnIndex("name")))

                cursor.close()
            }
        }

        private fun setGrainAlpha() { //解答表示用
            val tag = "setGrainAlpha"
            scaledGrain = Bitmap.createScaledBitmap(grainImg, (grainR * 2).toInt(), (grainR * 2).toInt(), false)

            val subAlpha = calcSubAlpha()
            if (subAlpha > 0) {
                setSubAlpha(subAlpha)
            }
        }

        private fun calcSubAlpha(): Int {
            val tag = "MyView/calcSubAlpha"
            val phase = doubleArrayOf(drawAnswerLength * 0.2, drawAnswerLength * 0.7)
            val timeInCue = ((now - initTime - marginTime) % drawAnswerLength).toInt() //制御のキー

            var subAlpha = 0
            when {
                timeInCue < phase[0].toInt() -> subAlpha = (255 * (1 - timeInCue / phase[0])).toInt()
                phase[1].toInt() < timeInCue -> subAlpha = (255 * (timeInCue - phase[1]) / (drawAnswerLength - phase[1])).toInt()
            }
            return subAlpha
        }

        private fun setGrainAlpha(releaseTime: Long) { //フェードアウト用
            fun calcSubAlpha(releaseTime: Long): Int {
                val tol = 500
                if (now - releaseTime > tol) {
                    resetThrough()
                    var result = ((now - releaseTime - tol) / 2f).toInt()
                    return result
                } else {
                    return 0
                }
            }

            val tag = "setGrainAlpha"
            scaledGrain = Bitmap.createScaledBitmap(grainImg, (grainR * 2).toInt(), (grainR * 2).toInt(), false)

            var subAlpha = 0
            if (releaseTime > -1) {
                subAlpha = calcSubAlpha(releaseTime)
            }
            if (subAlpha > 0) {
                setSubAlpha(subAlpha)
            }
        }

        private fun setSubAlpha(subAlpha: Int) {
            val w = scaledGrain?.width ?: 0
            val h = scaledGrain?.height ?: 0

            val pixels = IntArray(w * h)
            scaledGrain?.getPixels(pixels, 0, w, 0, 0, w, h)

            for (y in 0 until h) {
                for (x in 0 until w) {
                    var a = pixels[x + y * w]
                    var b = a
                    a = a.ushr(24) //alpha値

                    if (a > 0) {
                        a -= subAlpha
                        if (a < 0) a = 0 //alphaがマイナスになると予期せぬ表示になるので防止
                        a = a shl 24

                        b = b and 16777215

                        pixels[x + y * w] = a + b
                    }
                }
            }
            scaledGrain?.setPixels(pixels, 0, w, 0, 0, w, h)
        }

        private fun phase(particle: Particle): Int {
            return if (particle.diffFrames > particle.moveFrames || isReleasedOutsideButton || (!isStartGame && !isCmdSeq)) 1 else 0
        }

        internal inner class Particle(val x0: Float, val y0: Float, val canvas: Canvas) {
            val tag = "MyActivity.Particle"
            var grain = ArrayList<Grain>()
            val moveFrames: Long = 350
            var diffFrames: Long = 0
            var initFrame: Long = 0
            var v = 0.15

            init {
                initFrame = System.currentTimeMillis()
                for (i in 0..2) {
                    grain.add(Grain(x0, y0, true, null))
                    for (j in 0..2) {
                        grain.add(Grain(grain[i].origin.x, grain[i].origin.y, false, grain[i].step0))
                    }
                }
            }

            fun move(phase: Int) {
                diffFrames = System.currentTimeMillis() - initFrame

                when (phase) {
                    0 -> { //収束前
                        val param = (moveFrames - diffFrames) / (moveFrames.toFloat())
                        for (i in grain.indices) {
                            grain[i].x = grain[i].step1.x + grain[i].diff.x * param
                            grain[i].y = grain[i].step1.y + grain[i].diff.y * param
                        }
                    }
                    1 -> { //収束後
                        (grain.lastIndex downTo 0)
                                .filterNot { grain[it].isOrigin }
                                .forEach { grain.removeAt(it) }
                        for (i in grain.indices) {
                            val param = Math.cos(grain[i].paramAngle)
                            grain[i].x += (Math.cos(grain[i].baseAngle) * grain[i].circleR * param).toFloat()
                            grain[i].y += (Math.sin(grain[i].baseAngle) * grain[i].circleR * param).toFloat()
                            grain[i].paramAngle += v
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
                var paramAngle = Math.random() * pi2
                val baseAngle = Math.random() * pi2
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

                    if (isReleasedOutsideButton || !isStartGame) {
                        this.x = step1.x
                        this.y = step1.y
                    } else {
                        this.x = step0.x
                        this.y = step0.y
                    }
                }
            }

            private fun draw() {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
                for (gr in grain) {
                    canvas.drawBitmap(scaledGrain, gr.x - grainR, gr.y - grainR, paint)
                }
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }
        }

        //dotのリストを与えてそのリストが示す軌跡上にパーティクルを表示させる
        fun putParticles(throughDots: ThroughList, canvas: Canvas) {
            val tag = "MyView/putParticles"
            val interval = 25 * scale
            val size = FloatArray(throughDots.dots.lastIndex)
            for (i in 1..throughDots.dots.lastIndex) {
                val dotI0 = dots[throughDots.dots[i]]
                val dotI1 = dots[throughDots.dots[i - 1]]
                if (dotI0 != null && dotI1 != null) {
                    size[i - 1] = Math.sqrt(Math.pow((dotI1.x - dotI0.x).toDouble(), 2.0) + Math.pow((dotI1.y - dotI0.y).toDouble(), 2.0)).toFloat()
                }
            }
            synchronized(locus) {
                locus.clear()
            }
            for (i in size.indices) {
                val dotI0 = dots[throughDots.dots[i]]
                val dotI1 = dots[throughDots.dots[i + 1]]
                if (dotI0 != null && dotI1 != null) {
                    val unitV = floatArrayOf((dotI1.x - dotI0.x) / size[i], (dotI1.y - dotI0.y) / size[i])
                    var x = dotI0.x
                    var y = dotI0.y

                    val sumLength = floatArrayOf(0f, 0f)
                    synchronized(locus) {
                        val absX = Math.abs(dotI1.x - dotI0.x)
                        val absY = Math.abs(dotI1.y - dotI0.y)
                        val dX = unitV[0] * interval
                        val dY = unitV[1] * interval
                        val dXa = absX * interval / size[i]
                        val dYa = absY * interval / size[i]
                        while (sumLength[0] <= absX && sumLength[1] <= absY) {
                            locus.add(Particle(x, y, canvas))
                            x += dX
                            y += dY
                            sumLength[0] += dXa
                            sumLength[1] += dYa
                        }
                    }
                }
            }
        }
        /*
        var lastTime: Long = -1
        var interTime: Long = -1
        var frames = 0
        var sumTimes = 0
        var fps = -1f
        fun drawFPS(canvas: Canvas) {
            val nowTime = System.currentTimeMillis()
            if (lastTime == -1.toLong()) {
                lastTime = nowTime
                interTime = lastTime
            } else {
                sumTimes += (1000f / (nowTime - lastTime)).toInt()
                lastTime = nowTime
                frames++
            }
            if (nowTime - interTime > 200) {
                fps = (sumTimes / frames).toFloat()

                interTime = nowTime
                frames = 0
                sumTimes = 0
            }
            if (fps > -1) {
                paint.textSize = 30 * scale
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("FPS:" + "%.2f".format(fps), 0f, offsetY * 2 - 120 * scale, paint)
            }
        }
        */
        private fun getCorrectStrings(c: Cursor): ArrayList<String> {
            val strings = ArrayList(Arrays.asList(*c.getString(c.getColumnIndex("sequence")).split(",".toRegex()).toTypedArray()))
            val correctStrings = if (c.isNull(c.getColumnIndex("correctSeq"))) null else ArrayList(Arrays.asList(*c.getString(c.getColumnIndex("correctSeq")).split(",".toRegex()).toTypedArray()))

            return if (correctStrings != null) {
                val tStrings = ArrayList<String>()
                for (i in correctStrings.indices) {
                    if (correctStrings[i] == "") {
                        tStrings.add(strings[i])
                    } else {
                        tStrings.add(correctStrings[i])
                    }
                }
                tStrings
            } else {
                strings
            }
        }

        private fun drawButton(canvas: Canvas) {
            val nextButtonWidth = (if (isStartGame && doShow) 200 else 150) * scale
            val redoButtonWidth = 150 * scale
            val retryButtonWidth = 170 * scale
            val buttonHeight = 100 * scale
            val margin = 40 * scale
            val buttonBaseline = 35;
            nextButtonPoint[0] = Point((offsetX * 2 - nextButtonWidth - margin).toInt(), (offsetY * 2 - buttonHeight - margin).toInt())
            nextButtonPoint[1] = Point((offsetX * 2 - margin).toInt(), (offsetY * 2 - margin).toInt())
            redoButtonPoint[0] = Point((margin).toInt(), (offsetY * 2 - buttonHeight - margin).toInt())
            redoButtonPoint[1] = Point((margin + redoButtonWidth).toInt(), (offsetY * 2 - margin).toInt())
            retryButtonPoint[0] = redoButtonPoint[0]
            retryButtonPoint[1] = Point((margin + retryButtonWidth).toInt(), (offsetY * 2 - margin).toInt())

            paint.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 45 * scale
            paint.style = Paint.Style.FILL
            val button0: Drawable = if (version >= 23) resources.getDrawable(R.drawable.button0, null) else resources.getDrawable(R.drawable.button0)
            val button1: Drawable = if (version >= 23) resources.getDrawable(R.drawable.button1, null) else resources.getDrawable(R.drawable.button1)
            val dNext: Drawable
            val dRedo: Drawable
            val dRetry: Drawable

            dNext =
                    if ((isOnNext[0] || isOnNext[1] || isOnNext[2]) && isTouch) {
                        button1
                    } else {
                        button0
                    }
            dNext.setBounds(nextButtonPoint[0]?.x ?: 0, nextButtonPoint[0]?.y ?: 0, nextButtonPoint[1]?.x ?: 0, nextButtonPoint[1]?.y ?: 0)
            dNext.draw(canvas)
            if (isStartGame && doShow) {
                canvas.drawText("BYPASS", (nextButtonPoint[0]?.x ?: 0) + nextButtonWidth / 2, (nextButtonPoint[1]?.y ?: 0) - buttonBaseline * scale, paint)
            } else {
                canvas.drawText("NEXT", (nextButtonPoint[0]?.x ?: 0) + nextButtonWidth / 2, (nextButtonPoint[1]?.y ?: 0) - buttonBaseline * scale, paint)
            }

            if (doShowRedo) {
                dRedo =
                        if ((isOnRedo[0] || isOnRedo[1] || isOnRedo[2]) && isTouch) {
                            button1
                        } else {
                            button0
                        }
                dRedo.setBounds(redoButtonPoint[0]?.x ?: 0, redoButtonPoint[0]?.y ?: 0, redoButtonPoint[1]?.x ?: 0, redoButtonPoint[1]?.y ?: 0)
                dRedo.draw(canvas)
                canvas.drawText("REDO", (redoButtonPoint[0]?.x ?: 0) + redoButtonWidth / 2, (redoButtonPoint[1]?.y ?: 0) - buttonBaseline * scale, paint)
            } else if (isEndGame) {
                dRetry =
                        if (isOnRetry[0] || isOnRetry[1] || isOnRetry[2]) {
                            if (version >= 23) resources.getDrawable(R.drawable.button1, null) else resources.getDrawable(R.drawable.button1)
                        } else {
                            if (version >= 23) resources.getDrawable(R.drawable.button0, null) else resources.getDrawable(R.drawable.button0)
                        }
                dRetry.setBounds(retryButtonPoint[0]?.x ?: 0, retryButtonPoint[0]?.y ?: 0, retryButtonPoint[1]?.x ?: 0, retryButtonPoint[1]?.y ?: 0)
                dRetry.draw(canvas)
                canvas.drawText("RETRY", (retryButtonPoint[0]?.x ?: 0) + retryButtonWidth / 2, (retryButtonPoint[1]?.y ?: 0) - buttonBaseline * scale, paint)
            }
        }

        private fun drawCount(canvas: Canvas) {
            paint.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
            paint.textSize = 40f
            paint.textAlign = Paint.Align.RIGHT
            val x = (offsetX * 2.0 - 40.0 * scale).toFloat()
            val y = (offsetY * 2.0 - 150.0 * scale).toFloat()

            canvas.drawText("HACK:" + viewCount, x, y, paint)
        }

        private fun hexagonPath(origin: PointF, r: Float): Path {
            val path = Path()

            for (i in 0..6) {
                if (i == 0) {
                    path.moveTo((Math.cos(cr * (i - 0.5)) * r + origin.x).toFloat(), (Math.sin(cr * (i - 0.5)) * r + origin.y).toFloat())
                } else {
                    path.lineTo((Math.cos(cr * (i - 0.5)) * r + origin.x).toFloat(), (Math.sin(cr * (i - 0.5)) * r + origin.y).toFloat())
                }
            }

            return path
        }

        var upTime: Long = 0
        var leftTime: Long = 0
        private fun drawTime(currentTime: Long, canvas: Canvas) {
            val tag = "drawTime"
            leftTime = (defTime - ((if (isEndGame) holdTime else currentTime) - initTime)) / 10

            if (leftTime <= 0 && isFirstTimeUp) {
                for (i in 0 until qTotal) {
                    Log.d(tag, "q[" + i + "]:" + judgeLocus(answerThroughList[i]!!, throughDots[i] ?: ThroughList()))
                }
                holdTime = now
                isEndGame = true
                isFirstTimeUp = false
            }

            paint.style = Paint.Style.FILL
            if (doShow) {
                paint.textSize = 60 * scale
                paint.color = Color.rgb(220, 190, 50)
                val dispTime = if (isEndGame) upTime else leftTime

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%02d".format(dispTime / 100), offsetX - 3, offsetY / 3, paint)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(":", offsetX, offsetY / 3, paint)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("%02d".format(dispTime % 100), offsetX + 3, offsetY / 3, paint)

                val barWidth = (offsetX * 0.7 / defTime).toFloat() * leftTime.toFloat() * 10f
                paint.style = Paint.Style.FILL
                canvas.drawRect(offsetX - barWidth, (offsetY / 2.7).toFloat(), offsetX + barWidth, (offsetY / 2.55).toFloat(), paint)
            } else {
                paint.textSize = 70 * scale
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%02d".format(upTime / 100), offsetX - 5, offsetY / 9, paint)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(":", offsetX, offsetY / 9, paint)
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("%02d".format(upTime % 100), offsetX + 5, offsetY / 9, paint)
            }
        }

        private fun drawQueNumber(currentTime: Long, marginTime: Long, r: Int, g: Int, b: Int, canvas: Canvas) {
            val hexRadius = offsetX / 10
            val hexMargin = 5f
            val totalMargin = hexMargin * (qTotal - 1)
            val width = (qTotal - 1) * (offsetX / 5)
            var x: Float
            var y: Float
            val arrayNormal = intArrayOf(Color.argb(140, r, g, b), Color.argb(70, r, g, b), Color.argb(45, r, g, b), Color.argb(40, r, g, b), Color.argb(45, r, g, b), Color.argb(70, r, g, b), Color.argb(140, r, g, b))
            val arrayStrong = intArrayOf(Color.argb(255, r, g, b), Color.argb(130, r, g, b), Color.argb(85, r, g, b), Color.argb(75, r, g, b), Color.argb(85, r, g, b), Color.argb(130, r, g, b), Color.argb(255, r, g, b))
            val positions = floatArrayOf(0f, 0.15f, 0.35f, 0.5f, 0.65f, 0.85f, 1f)

            for (i in 0 until qTotal) {
                x = offsetX - (width / 2 + totalMargin) + i.toFloat() * (hexRadius + hexMargin) * 2f
                y = (offsetY / 7.5).toFloat()
                val origin = PointF(x, y)
                val lgNormal = LinearGradient(x, y - hexRadius, x, y + hexRadius, arrayNormal, positions, Shader.TileMode.CLAMP)
                val lgStrong = LinearGradient(x, y - hexRadius, x, y + hexRadius, arrayStrong, positions, Shader.TileMode.CLAMP)

                paint.color = Color.BLACK
                if (isStartGame) {
                    when (i) {
                        in 0 until qNum -> {
                            //paint.setShader(lgNormal)
                            paint.shader = lgNormal
                            paint.style = Paint.Style.FILL
                            canvas.drawPath(hexagonPath(origin, hexRadius), paint)
                            //paint.setShader(null)
                            paint.shader = null

                            paint.color = Color.argb(140, r, g, b)
                        }
                        qNum -> {
                            if ((isReleasedOutsideButton && throughDots[qTotal - 1]?.dots?.size ?: 0 > 0)) {
                                //paint.setShader(lgNormal)
                                paint.shader = lgNormal
                                paint.style = Paint.Style.FILL
                                canvas.drawPath(hexagonPath(origin, hexRadius), paint)
                                //paint.setShader(null)
                                paint.shader = null

                                paint.color = Color.argb(140, r, g, b)
                            } else {
                                //paint.setShader(lgStrong)
                                paint.shader = lgStrong
                                paint.style = Paint.Style.FILL
                                canvas.drawPath(hexagonPath(origin, hexRadius), paint)
                                //paint.setShader(null)
                                paint.shader = null

                                paint.color = Color.rgb(r, g, b)
                            }
                        }
                        else -> {
                            paint.color = Color.BLACK
                            paint.style = Paint.Style.FILL
                            canvas.drawPath(hexagonPath(origin, hexRadius), paint)

                            paint.color = Color.argb(80, r, g, b)
                        }
                    }
                } else {
                    if (i.toLong() == (currentTime - marginTime) / drawAnswerLength && currentTime > marginTime) {
                        //paint.setShader(lgStrong)
                        paint.shader = lgStrong
                        paint.style = Paint.Style.FILL
                        canvas.drawPath(hexagonPath(origin, hexRadius), paint)
                        //paint.setShader(null)
                        paint.shader = null

                        paint.color = Color.rgb(r, g, b)
                    } else {
                        paint.color = Color.BLACK
                        paint.style = Paint.Style.FILL
                        canvas.drawPath(hexagonPath(origin, hexRadius), paint)

                        paint.color = Color.argb(140, r, g, b)
                    }
                }
                paint.strokeJoin = Paint.Join.BEVEL
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE
                canvas.drawPath(hexagonPath(origin, hexRadius), paint)
                paint.strokeWidth = 0f
            }
        }

        var cmdLimitTime = initTime + 2000
        private fun listenCmd(canvas: Canvas) {
            if (now > cmdLimitTime && !isTouch) {
                isCmdSeq = false
                when(hackMode) {
                    MODE_SIMPLE -> drawAnswerLength *= 2
                    MODE_COMPLEX -> drawAnswerLength /= 3
                }
                resetLocus()
                throughDots[0] = ThroughList()
                releaseTime = -1
                initTime = now
            } else {
                drawDialog(canvas)
            }
        }

        val MODE_NOMAL = "nomal"
        val MODE_SIMPLE = "simple"
        val MODE_COMPLEX = "complex"
        var hackMode = MODE_NOMAL
        var isValidCmd = false
        private fun judgeCmd() {
            val tag = "judgeCmd"
            val simple = arrayListOf(2, 1)
            val complex = arrayListOf(4, 3, 0, 2)
            when {
                judgeLocus(ThroughList(simple), throughDots[0] ?: ThroughList()) -> {
                    hackMode = MODE_SIMPLE
                    isValidCmd = true
                    Log.d(tag, "simple")
                }
                judgeLocus(ThroughList(complex), throughDots[0] ?: ThroughList()) -> {
                    hackMode = MODE_COMPLEX
                    isValidCmd = true
                    Log.d(tag, "complex")
                }
                else -> isValidCmd = false
            }
        }

        private fun drawDialog(canvas: Canvas) {
            if (releaseTime > -1 && now < cmdLimitTime && isValidCmd) {
                val width = when(hackMode) {
                    MODE_SIMPLE -> 400 * scale
                    MODE_COMPLEX -> 440 * scale
                    else -> 0F
                }
                val height = 170 * scale
                val bgColor = 0xff2f1b00.toInt()
                val textColor =  0xffe1b23a.toInt()
                val borderColor = 0xff63502d.toInt()
                val box = Rect((offsetX - width / 2).toInt(), (offsetY - height / 2).toInt(), (offsetX + width / 2).toInt(), (offsetY + height / 2).toInt())

                paint.color = bgColor
                paint.style = Paint.Style.FILL
                canvas.drawRect(box, paint)

                paint.color = borderColor
                paint.strokeWidth = 5f
                paint.style = Paint.Style.STROKE
                canvas.drawRect(box, paint)
                paint.strokeWidth = 0f

                paint.color = textColor
                paint.style = Paint.Style.FILL
                paint.textSize = 50 * scale
                paint.textAlign = Paint.Align.CENTER
                val text = when(hackMode) {
                    MODE_SIMPLE -> "SIMPLE HACK"
                    MODE_COMPLEX -> "COMPLEX HACK"
                    else -> ""
                }
                canvas.drawText(text, offsetX, offsetY + 18 * scale, paint)
            }
        }

        var preCue = -1
        private fun drawAnswer(initTime: Long, currentTime: Long, canvas: Canvas) {
            setGrainAlpha()

            var cue: Int = -1
            val diffTIme = currentTime - initTime - marginTime

            fun drawAnswerText() {
                paint.color = Color.argb(255 - calcSubAlpha(), 255, 255, 255)
                paint.textSize = 80 * scale
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(correctStr[cue], offsetX, offsetY / 3, paint)
            }

            if (diffTIme >= 0) {
                cue = (diffTIme / drawAnswerLength).toInt()
            }
            if (-1 < cue) {
                if (cue < qTotal) {
                    if (preCue != cue && (gameMode == 0 || gameMode == 2)) {
                        putParticles(answerThroughList[cue]!!, canvas)
                    }
                    if (gameMode == 0 || gameMode == 1) {
                        drawAnswerText()
                    }
                } else {
                    drawFlash(currentTime, canvas)
                }
            }

            preCue = cue
        }

        var initTimeFlash: Long = 0
        var isFirstFlash = true
        private fun drawFlash(currentTime: Long, canvas: Canvas) {
            val tag ="MyView/drawFlash"
            if (isFirstFlash) {
                resetLocus()
                initTimeFlash = System.currentTimeMillis()
                isFirstFlash = false
            }

            var cue: Int
            val interval = 680
            val margin = 10
            val dT = (currentTime - initTimeFlash).toInt()
            val alpha: Int

            cue = dT / interval
            if (dT > interval * 2.5) {
                cue++
            }

            when (cue) {
                0 -> {
                    alpha =
                            if (dT < margin) {
                                150 * dT / margin
                            } else {
                                150 - 150 * (dT - margin) / (interval - margin)
                            }
                    paint.color = Color.argb(alpha, 220, 175, 50)
                }
                1 -> {
                    alpha =
                            if (dT < margin) {
                                200 * dT / cue / margin
                            } else {
                                200 - 200 * (dT - margin) / cue / (interval - margin)
                            }
                    paint.color = Color.argb(alpha, 220, 175, 50)
                }
                2 -> {
                    alpha =
                            if (dT < margin) {
                                255 * dT / cue / margin
                            } else {
                                255
                            }
                    paint.color = Color.argb(alpha, 255, 255, 255)
                }
                else -> {
                    initTime = System.currentTimeMillis()
                    isStartGame = true
                }
            }
            paint.style = Paint.Style.FILL
            canvas.drawRect(0.0f, 0.0f, offsetX * 2, offsetY * 2, paint)
        }

        var isRecorded = false
        private fun recordResult() {
            val tag = "MyView/recordResult"

            var correctNum = 0
            val isCorrect = Array(qTotal, { i -> false })
            for (i in 0 until qTotal) {
                if (judgeLocus(answerThroughList[i]!!, throughDots[i]!!)) {
                    isCorrect[i] = true
                    correctNum++
                }
            }
            correctNum = isCorrect.count { it == true }
            Log.d(tag, "correctNum: $correctNum")

            var cursor: Cursor
            cursor =
                    if (qTotal > 1) {
                        db.query(DBHelper.TABLE_NAME2, null, "id = ${randomVal + 1}", null, null, null, null, null)
                    } else {
                        db.query(DBHelper.TABLE_NAME1, null, "id = ${randomVal + 1}", null, null, null, null, null)
                    }
            cursor.moveToFirst()

            var totalNumber = cursor.getInt(cursor.getColumnIndex("total_number"))
            val contentValues = ContentValues()
            if (correctNum == qTotal) {
                contentValues.put("correct_number", if (totalNumber > 0) cursor.getInt(cursor.getColumnIndex("correct_number")) + 1 else 1)
            } else {
                contentValues.put("correct_number", if (totalNumber > 0) cursor.getInt(cursor.getColumnIndex("correct_number")) else 0)
            }
            contentValues.put("total_number", if (totalNumber > 0) totalNumber + 1 else 1)
            if (qTotal > 1) {
                val c = db.query(DBHelper.TABLE_NAME2, arrayOf("sequence"), "id = ${randomVal + 1}", null, null, null, null, null)
                c.moveToFirst()
                val shapesSplit = c.getString(0).split(",".toRegex()).toTypedArray()
                c.close()
                db.update(DBHelper.TABLE_NAME2, contentValues, "id = ${randomVal + 1}", null)
                for (i in answerThroughList.indices) {
                    val cursorOnShaper = db.rawQuery("select id from ${DBHelper.TABLE_NAME1} where name = '${shapesSplit[i].replace("'", "''")}';", null)
                    cursorOnShaper.moveToFirst()
                    cursor = db.query(DBHelper.TABLE_NAME1, null, "id = ${cursorOnShaper.getInt(0)}", null, null, null, null, null)
                    cursor.moveToFirst()

                    totalNumber = cursor.getInt(cursor.getColumnIndex("total_number"))
                    val cv = ContentValues()
                    if (isCorrect[i]) {
                        cv.put("correct_number", if (totalNumber > 0) cursor.getInt(cursor.getColumnIndex("correct_number")) + 1 else 1)
                        Log.d(tag, "correct: ${shapesSplit[i]}")
                    } else {
                        cv.put("correct_number", if (totalNumber > 0) cursor.getInt(cursor.getColumnIndex("correct_number")) else 0)
                        Log.d(tag, "fault: ${shapesSplit[i]}")
                    }
                    cv.put("total_number", if (totalNumber > 0) totalNumber + 1 else 1)
                    db.update(DBHelper.TABLE_NAME1, cv, "id = ${cursor.getInt(0)}", null)
                    cursorOnShaper.close()
                }
            } else {
                db.update(DBHelper.TABLE_NAME1, contentValues, "id = ${randomVal + 1}", null)
            }
            cursor.close()
            isRecorded = true
        }

        private fun drawResult(margin: Long, initTime: Long, currentTime: Long, canvas: Canvas) {
            if (currentTime > initTime + margin) {
                drawTime(now, canvas)

                val blue = Color.rgb(2, 255, 197)
                val red = Color.RED
                var drawColor: Int
                var correctNum = 0
                for (i in 0 until qTotal) {
                    val answerPath = Path()
                    val hexaRadius = offsetX / 8
                    val hexaMargin = 10 * scale
                    val totalMargin = hexaMargin * (qTotal - 1)
                    val height = (qTotal - 1) * (offsetX / 5)
                    val x = offsetX / 6
                    val y = offsetY * 2 / 3 - (height / 2 + totalMargin) + i.toFloat() * (hexaRadius + hexaMargin) * 2f
                    val giveOrigin = PointF(x, y)
                    if (judgeLocus(answerThroughList[i]!!, throughDots[i] ?: ThroughList())) {
                        drawColor = blue
                        correctNum++
                    } else {
                        drawColor = red
                    }

                    paint.color = Color.argb(80, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor))
                    paint.style = Paint.Style.FILL
                    canvas.drawPath(hexagonPath(giveOrigin, hexaRadius), paint)

                    paint.color = Color.argb(255, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor))
                    paint.style = Paint.Style.STROKE
                    canvas.drawPath(hexagonPath(giveOrigin, hexaRadius), paint)

                    for (j in answerThroughList[i]!!.dots.indices) {
                        if (j == 0) {
                            answerPath.moveTo(x - hexaRadius + dots[answerThroughList[i]!!.dots[j]]!!.x / 8, y + (dots[answerThroughList[i]!!.dots[j]]!!.y - offsetY * 1.2).toFloat() / 8)
                        } else {
                            answerPath.lineTo(x - hexaRadius + dots[answerThroughList[i]!!.dots[j]]!!.x / 8, y + (dots[answerThroughList[i]!!.dots[j]]!!.y - offsetY * 1.2).toFloat() / 8)
                        }
                    }
                    paint.strokeWidth = 3 * scale
                    canvas.drawPath(answerPath, paint)

                    paint.style = Paint.Style.FILL
                    paint.strokeWidth = 1f
                    paint.textSize = 70 * scale
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(correctStr[i], x * 2, giveOrigin.y + 25 * scale, paint)
                    paint.textSize = 50 * scale
                    paint.textAlign = Paint.Align.RIGHT
                    paint.color = Color.WHITE
                    if (passTime[i] > -1) {
                        canvas.drawText("${passTime[i] / 100}:${passTime[i] % 100}", offsetX * 2 - 5 * scale, giveOrigin.y + 20 * scale, paint)
                    }
                }

                paint.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
                paint.textSize = 40 * scale
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(getText(R.string.bonus_hack).toString(), offsetX, offsetY * 4 / 3, paint)
                canvas.drawText(getText(R.string.bonus_speed).toString(), offsetX, offsetY * 5 / 3, paint)
                paint.color = Color.WHITE
                paint.textSize = 120 * scale
                canvas.drawText(calcBonus(level, false, correctNum.toLong(), null) + "%", offsetX, offsetY * 5 / 3 - 80 * scale, paint)
                canvas.drawText(calcBonus(level, true, correctNum.toLong(), upTime) + "%", offsetX, offsetY * 2 - 80 * scale, paint)
            }
        }

        private fun calcBonus(level: Int, isSpeed: Boolean, correctNum: Long, time: Long?): String {
            var bonus: Long
            val total = difficulty[level].qs.toLong()
            val isClearAll = total == correctNum
            when {
                isSpeed -> bonus =
                        if (isClearAll) {
                            Math.round((time ?: 0) * 1000.0 / difficulty[level].time)
                        } else {
                            0L
                        }
                isClearAll -> {
                    bonus =
                            when (level) {
                                in 0..1 -> 38
                                2 -> 60
                                in 3..5 -> 85
                                in 6..7 -> 120
                                8 -> 162
                                else -> 0
                            }
                    bonus = Math.round(bonus.toDouble() * correctNum / difficulty[level].qs)
                }
                else -> bonus = correctNum * 10
            }
            return "$bonus"
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
                    (0 until answer.dots.lastIndex).map { intArrayOf(answer.dots[it], answer.dots[it + 1]) }
            var passedPaths =
                    (0 until through.dots.lastIndex).mapTo(ArrayList()) { intArrayOf(through.dots[it], through.dots[it + 1]) }
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
            synchronized(locus) {
                locus.add(Particle(x, y, canvas))

                if (doCD) {
                    setCollision(x, y, x, y)
                }
                locusPath.moveTo(x, y)
            }
        }

        private fun setLocus(x: Float, y: Float, doCD: Boolean, canvas: Canvas) {
            synchronized(locus) {
                locus.add(Particle(x, y, canvas))

                if (doCD) {
                    setCollision(x, y, locus[locus.size - 2].x0, locus[locus.size - 2].y0)
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
                    if (difX * difX + difY * difY < r * r && state) {
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
            if (collisionDot != -1 && (throughDots[qNum]!!.dots.size < 1 || throughDots[qNum]!!.dots[throughDots[qNum]!!.dots.size - 1] != collisionDot)) {
                throughDots[qNum]?.dots?.add(collisionDot)
                if (doVibrate) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(30)
                }
                previousDot = collisionDot
            }
        }

        private fun resetLocus() {
            locusPath.reset()
            synchronized(locus) {
                locus.clear()
            }
        }

        private fun resetThrough() {
            for (i in 0..10) {
                isThrough[i] = false
            }
        }

        var downX = 0f
        var downY = 0f
        var isTouch = false
        var isReleasedOutsideButton = false
        var doShowRedo = false
        var isOnNext = Array(3, {i -> false})
        var isOnRedo = Array(3, {i -> false})
        var isOnRetry = Array(3, {i -> false})
        var releaseTime: Long = -1
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val tag = "onTouchEvent"

            val lim = 15 * scale
            val upX: Float
            val upY: Float
            when (event.action) {
                MotionEvent.ACTION_DOWN //タッチ
                -> {
                    downX = event.x
                    downY = event.y
                    isTouch = true
                    isOnNext[0] = nextButtonPoint[0]?.x ?: -1 <= downX && downX <= nextButtonPoint[1]?.x ?: -1 && nextButtonPoint[0]?.y ?: -1 <= downY && downY <= nextButtonPoint[1]?.y ?: -1
                    isOnRedo[0] = doShowRedo && redoButtonPoint[0]?.x ?: -1 <= downX && downX <= redoButtonPoint[1]?.x ?: -1 && redoButtonPoint[0]?.y ?: -1 <= downY && downY <= redoButtonPoint[1]?.y ?: -1
                    isOnRetry[0] = isEndGame && !doShowRedo && retryButtonPoint[0]?.x ?: -1 <= downX && downX <= retryButtonPoint[1]?.x ?: -1 && retryButtonPoint[0]?.y ?: -1 <= downY && downY <= retryButtonPoint[1]?.y ?: -1
                    if (!isEndGame) releaseTime = -1
                    if (!isOnNext[0] && isStartGame && !isEndGame) {
                        if (isReleasedOutsideButton) {
                            resetLocus()
                            resetThrough()
                            isReleasedOutsideButton = false
                        }
                        if (!isOnRedo[0]) setLocusStart(downX, downY, true, canvas!!)
                    }
                    if (isCmdSeq) {
                        resetLocus()
                        resetThrough()
                        throughDots[0] = ThroughList()
                        setLocusStart(downX, downY, true, canvas!!)
                    }
                }
                MotionEvent.ACTION_MOVE //スワイプ
                -> {
                    val currentX = event.x
                    val currentY = event.y
                    isOnNext[1] = isOnNext[0] && nextButtonPoint[0]?.x ?: -1 <= currentX && currentX <= nextButtonPoint[1]?.x ?: -1 && nextButtonPoint[0]?.y ?: -1 <= currentY && currentY <= nextButtonPoint[1]?.y ?: -1
                    isOnRedo[1] = doShowRedo && isOnRedo[0] && redoButtonPoint[0]?.x ?: -1 <= currentX && currentX <= redoButtonPoint[1]?.x ?: -1 && redoButtonPoint[0]?.y ?: -1 <= currentY && currentY <= redoButtonPoint[1]?.y ?: -1
                    isOnRetry[1] = isEndGame && !doShowRedo && isOnRetry[0] && retryButtonPoint[0]?.x ?: -1 <= currentX && currentX <= retryButtonPoint[1]?.x ?: -1 && retryButtonPoint[0]?.y ?: -1 <= currentY && currentY <= retryButtonPoint[1]?.y ?: -1
                    if (isCmdSeq || (isStartGame && !isEndGame)) {
                        if (currentX + lim < downX || downX + lim < currentX || currentY + lim < downY || downY + lim < currentY) {
                            if (locus.size == 0) {
                                setLocusStart(currentX, currentY, true, canvas!!)
                            } else {
                                setLocus(currentX, currentY, true, canvas!!)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP //リリース
                -> {
                    upX = event.x
                    upY = event.y
                    isTouch = false
                    isOnNext[2] = isOnNext[0] &&
                            nextButtonPoint[0]?.x ?: -1 <= upX && upX <= nextButtonPoint[1]?.x ?: -1 && nextButtonPoint[0]?.y ?: -1 <= upY && upY <= nextButtonPoint[1]?.y ?: -1
                    isOnRedo[2] = doShowRedo && isOnRedo[0] &&
                            redoButtonPoint[0]?.x ?: -1 <= upX && upX <= redoButtonPoint[1]?.x ?: -1 && redoButtonPoint[0]?.y ?: -1 <= upY && upY <= redoButtonPoint[1]?.y ?: -1
                    isOnRetry[2] = isEndGame && !doShowRedo && isOnRetry[0] &&
                            retryButtonPoint[0]?.x ?: -1 <= upX && upX <= retryButtonPoint[1]?.x ?: -1 && retryButtonPoint[0]?.y ?: -1 <= upY && upY <= retryButtonPoint[1]?.y ?: -1
                    if (!isOnNext[2] && !isOnRedo[2] && !isOnRetry[2] && isStartGame && !isEndGame) {
                        isReleasedOutsideButton = true
                        releaseTime = now
                        var tPassTime = (now - initTime) / 10
                        for (i in 0 until qNum) {
                            tPassTime -= passTime[i]
                        }
                        passTime[qNum] = tPassTime
                        Log.d(tag, "throughDots: ${throughDots[qNum]?.dots?.joinToString(",")}")
                        resetLocus()
                        if (throughDots[qNum]?.dots?.size ?: 0 > 0) {
                            putParticles(throughDots[qNum]!!, canvas!!)
                        }
                        qNum++
                        if (qTotal > qNum) {
                            doShowRedo = true
                        } else {
                            holdTime = now
                            upTime = (defTime - (holdTime - initTime)) / 10
                            isEndGame = true
                        }
                    }
                    if (isCmdSeq) {
                        releaseTime = now
                        cmdLimitTime = releaseTime + 1000
                        judgeCmd()
                    }
                    if (isOnNext[2]) {
                        if (!isStartGame) {
                            startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        } else if (doShow) {
                            now = initTime + defTime
                            holdTime = now
                            pressButtonTime = System.currentTimeMillis()
                            upTime = (defTime - (pressButtonTime - initTime)) / 10
                            isPressedButton = true
                        } else {
                            startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        }
                    }
                    if (isOnRedo[2]) {
                        isEndGame = false
                        if (qNum != 0) {
                            qNum--
                            throughDots[qNum] = ThroughList()
                            if (qNum == 0) {
                                doShowRedo = false
                            }
                        }
                    }
                    if (isOnRetry[2]) {
                        val intent = Intent(this@MainActivity, MainActivity::class.java)
                        intent.putExtra("isRetry", true)
                        intent.putExtra("retryLevel", level)
                        intent.putExtra("retryValue", randomVal)
                        startActivity(intent)
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