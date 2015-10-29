package jp.org.example.geckour.glyph

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker

import java.util.ArrayList
import java.util.Arrays

import kotlin.concurrent.timer


class MyActivity : Activity() {
    internal val version: Int = Build.VERSION.SDK_INT
    internal var min = 0
    internal var max = 8
    internal var viewCount = 0
    internal var receivedLevel = -1
    internal var receivedValue = -1
    internal var offsetX: Float = 0.toFloat()
    internal var offsetY: Float = 0.toFloat()
    internal var scale: Float = 0.toFloat()
    internal var sp: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tag = "onCreate"
        sp = PreferenceManager.getDefaultSharedPreferences(this)

        val actionBar = actionBar
        actionBar?.hide()

        if (sp?.getInt("viewCount", -1) != -1) {
            viewCount = sp?.getInt("viewCount", 0) ?: 0
        } else {
            viewCount = 1
        }
        sp?.edit()?.putInt("viewCount", viewCount + 1)?.apply()

        try {
            min = Integer.parseInt(sp?.getString("min_level", "0"))
            Log.v(tag, "min:" + min)
        } catch (e: Exception) {
            Log.e(tag, "Can't translate minimum-level to int.")
        }

        try {
            max = Integer.parseInt(sp?.getString("max_level", "8"))
            Log.v(tag, "max:" + max)
        } catch (e: Exception) {
            Log.e(tag, "Can't translate maximum-level to int.")
        }

        val intent = intent
        if (intent.getBooleanExtra("isRetry", false)) {
            receivedLevel = intent.getIntExtra("retryLevel", -1)
            receivedValue = intent.getIntExtra("retryValue", -1)
        }

        /*if (Build.VERSION_CODES.HONEYCOMB < Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }*/
        setContentView(R.layout.activity_my)
/*
        val t: Tracker? = (application as Analytics).getTracker(Analytics.TrackerName.APP_TRACKER)
        t?.setScreenName("MyActivity")
        t?.send(HitBuilders.AppViewBuilder().build())
        */
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val tag = "onWindowFocusChanged"

        if (findViewById(R.id.root) != null) {
            val r = findViewById(R.id.root) as RelativeLayout
            offsetX = (r.width / 2).toFloat()
            offsetY = (r.height / 2).toFloat()
            scale = offsetY * 2 / 1280
        }

        val view: MyView = MyView(this)
        setContentView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.my, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_settings) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        val tag = "onActivityResult"
        if (requestCode == 0) {
            //Pref.javaからの戻り値の場合
            if (resultCode == Activity.RESULT_OK) {
                Log.v(tag, "setting is changed.")
            }
        }
    }

    public fun getViewSize(activity: Activity): Point {
        var point: Point = Point()
        activity.windowManager.defaultDisplay.getSize(point)
        return point
    }

    internal inner class MyView(context: Context) : View(context) {
        var state = true
        var gameMode: Int = 0
        var level: Int = 0
        var randomVal: Int = 0
        var p = Paint()
        var cr = Math.PI / 3
        var radius: Double = 0.toDouble()
        var dotDiam: Int = 0
        var grainR: Float = 0.toFloat()
        var dots = arrayOfNulls<PointF>(11)
        var Locus = ArrayList<Particle>()
        var locusPath = Path()
        var now: Long = 0
        var isThrough = BooleanArray(11)
        var throughList: Array<ThroughList?>
        var answerThroughList: Array<ThroughList?>
        var qTotal = 0
        var qNum = 0
        var defTime = 20000
        var initTime: Long = 0
        var showAnswerLength = 1500
        var marginTime: Long = 1000
        var pressButtonTime: Long = 0
        var isFirstDraw = true
        var isFirstTimeUp = true
        var doVibrate = false
        var showCountView = false
        var difficulty = ArrayList<Difficulty>()
        var isStartGame = false
        var isEndGame = false
        var doShow = true
        var isPressedButton = false
        var typeface: Typeface
        var correctStr: ArrayList<String>
        var holdTime: Long = 0
        var nextButtonPoint = arrayOfNulls<Point>(2)
        var retryButtonPoint = arrayOfNulls<Point>(2)
        var dbHelper: DBHelper
        var db: SQLiteDatabase
        var previousDot = -1
        var passTime: LongArray
        val grainImg: Bitmap
        var scaledGrain: Bitmap? = null
        var dotTrue: Bitmap? = null
        var dotFalse: Bitmap? = null
        var c: Canvas? = null

        init {
            val tag = "MyView"
            dbHelper = DBHelper(context)
            db = dbHelper.readableDatabase

            var giveTime = 20000
            var giveQs = 1
            //int giveBonus = 40;
            var giveBonus = 38
            for (i in 0..8) {
                if (i > 3) {
                    giveTime -= 1000
                }
                if (i == 2 || i == 3 || i == 6 || i == 8) {
                    giveQs++
                }
                /*if (i > 1) {
                    giveBonus += 5;
                }*/
                if (i == 2) {
                    giveBonus = 60
                }
                if (i == 3) {
                    giveBonus = 85
                }
                if (i == 6) {
                    giveBonus = 120
                }
                if (i == 8) {
                    giveBonus = 162
                }
                difficulty.add(i, Difficulty(giveQs, giveTime, giveBonus))
            }
            gameMode = Integer.parseInt(sp?.getString("gamemode", "0") ?: "0")
            doVibrate = sp?.getBoolean("doVibrate", false) ?: false
            showCountView = sp?.getBoolean("showCountView", false) ?: false
            level = if (receivedLevel > -1) receivedLevel else (Math.random() * (max - min + 1) + min).toInt()
            //level = 8;
            qTotal = difficulty[level].qs
            Log.v(tag, "qTotal:" + qTotal)
            passTime = LongArray(qTotal)
            for (i in 0..qTotal - 1) {
                passTime[i] = -1
            }
            defTime = difficulty[level].time
            /*
            Cursor tempC = db.query(DBHelper.TABLE_NAME2, null, null, null, null, null, null);
            tempC.moveToLast();
            long tempMax = tempC.getLong(0);
            tempC.moveToFirst();
            OutputStream out;
            try {
                out = openFileOutput("list.txt", MODE_APPEND);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));

                for (int i = 0; i < tempMax; i++) {
                    ArrayList<String> tempStrings = getCorrectStrings(tempC);
                    for (String s : tempStrings) {
                        writer.append(s + ",");
                    }
                    writer.append("\n");
                    if (i == tempMax - 1) {
                        writer.close();
                    }
                    tempC.moveToNext();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
            var c1 = db.query(DBHelper.TABLE_NAME1, null, null, null, null, null, null)
            var c2 = db.query(DBHelper.TABLE_NAME2, null, null, null, null, null, null)
            if (qTotal > 1) {

                val c3 = db.query(DBHelper.TABLE_NAME2, null, "level = " + qTotal, null, null, null, null)
                c3.moveToFirst()
                val min = c3.getLong(0)
                c3.moveToLast()
                val max = c3.getLong(0)
                randomVal = if (receivedValue > -1) receivedValue else (Math.random() * (max - min + 1) + min).toInt() - 1
                //int randomVal = 0;
                c2.moveToPosition(randomVal)
                Log.v(tag, "randomVal:$randomVal, level:$level")
                throughList = arrayOfNulls<ThroughList>(qTotal)
                answerThroughList = arrayOfNulls<ThroughList>(qTotal)
                val shapesSplit = c2.getString(2).split(",".toRegex()).toTypedArray()
                for (s in shapesSplit) {
                    Log.v(tag, "shapesSplit: " + s)
                }
                for (i in 0..qTotal - 1) {
                    throughList[i] = ThroughList()
                    val c = db.rawQuery("select * from " + DBHelper.TABLE_NAME1 + " where name = '" + shapesSplit[i] + "';", null)
                    c.moveToFirst()
                    //Log.v(tag, "shaper name: " + c.getString(1));
                    val dotsSplit = c.getString(c.getColumnIndex("path")).split(",".toRegex()).toTypedArray()
                    answerThroughList[i] = ThroughList(dotsSplit)
                    c.close()
                }
                correctStr = getCorrectStrings(c2)
            } else {
                c1.moveToLast()
                val max = c1.getLong(0)
                randomVal = if (receivedValue > -1) receivedValue else (Math.random() * max).toInt()
                //int randomVal = (int)max - 1;
                Log.v(tag, "randomVal:$randomVal, level:$level")
                throughList = arrayOfNulls<ThroughList>(qTotal)
                answerThroughList = arrayOfNulls<ThroughList>(qTotal)
                throughList[0] = ThroughList()
                c1.moveToPosition(randomVal)
                val dotsSplit = c1.getString(c1.getColumnIndex("path")).split(",".toRegex()).toTypedArray()
                answerThroughList[0] = ThroughList(dotsSplit)
                correctStr = ArrayList(Arrays.asList("" + c1.getString(c1.getColumnIndex("name"))))
            }
            p.isAntiAlias = true

            for (i in 0..10) {
                isThrough[i] = false
            }

            typeface = Typeface.createFromAsset(getContext().assets, "Coda-Regular.ttf")
            p.setTypeface(typeface)

            grainImg = BitmapFactory.decodeResource(resources, R.drawable.particle)

            now = System.currentTimeMillis()
            timer(initialDelay = 100, period = 25) {
                runOnUiThread {
                    if (state) {
                        invalidate() //再描画
                    }
                }
            }
        }

        public fun isEndLoad(): Boolean {
            return scale != 0.toFloat()
        }

        public override fun onDraw(c: Canvas) {
            this.c = c
            c.drawColor(if (version >= 23) resources.getColor(R.color.background, null) else resources.getColor(R.color.background))
            val tag = "onDraw"

            if (isEndLoad()) {
                if (isFirstDraw) {
                    radius = offsetX * 0.8
                    dotDiam = (radius / 4.5).toInt()
                    grainR = 15 * scale
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

                    initTime = now
                    isFirstDraw = false
                }

                if (showCountView) {
                    showCount()
                }

                if (!isStartGame) {
                    setGrainAlpha()
                    showAnswer(initTime, now)
                } else if (!isEndGame) {
                    setGrainAlpha(releaseTime)
                }

                if (doShow) {
                    for (i in 0..10) {
                        if (isThrough[i]) {
                            c.drawBitmap(dotTrue, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, p)
                        } else {
                            c.drawBitmap(dotFalse, dots[i]!!.x - dotDiam / 2, dots[i]!!.y - dotDiam / 2, p)
                        }
                    }
                    for (particle in Locus) {
                        particle.move()
                    }
                    if (!isStartGame || isReleased) {
                        p.color = Color.WHITE
                        p.strokeWidth = 3f
                        p.style = Paint.Style.STROKE
                        c.drawPath(locusPath, p)
                        p.strokeWidth = 0f
                    }
                }

                if (isStartGame && doShow) {
                    showTime(now)
                    showQueNumber(now - initTime, 0, 2, 255, 197)
                } else if (!isStartGame) {
                    showQueNumber(now - initTime, marginTime, 240, 150, 40)
                }

                showButton()
                //showFPS();

                if (isEndGame) {
                    if (now > holdTime + marginTime) {
                        doShow = false
                        showResult(marginTime, holdTime + marginTime, now)
                    }
                }

                now = if (isPressedButton) System.currentTimeMillis() - pressButtonTime + holdTime else System.currentTimeMillis()
            }
        }

        internal inner class ThroughList {
            var dots: ArrayList<Int>

            constructor() {
                dots = ArrayList<Int>()
            }

            constructor(argDots: ArrayList<Int>) {
                dots = ArrayList(argDots)
            }

            constructor(argDots: Array<String>) {
                val tag = "ThroughList"
                dots = ArrayList<Int>()
                for (s in argDots) {
                    try {
                        dots.add(Integer.parseInt(s))
                    } catch (e: Exception) {
                        Log.e(tag, e.message)
                    }

                }
            }
        }

        internal inner class Difficulty(argQs: Int, argTime: Int, argBonus: Int) {
            var qs = 0
            var time = 0
            var bonus = 0

            init {
                qs = argQs
                time = argTime
                bonus = argBonus
            }
        }

        fun setGrainAlpha() {
            scaledGrain = Bitmap.createScaledBitmap(grainImg, (grainR * 2).toInt(), (grainR * 2).toInt(), false)
            val w = scaledGrain?.width ?: 0
            val h = scaledGrain?.height ?: 0

            val pixels = IntArray(w * h)
            scaledGrain?.getPixels(pixels, 0, w, 0, 0, w, h)

            val subAlpha = calcSubAlpha()
            if (subAlpha != 0) {
                for (y in 0..h - 1) {
                    for (x in 0..w - 1) {
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

        private fun calcSubAlpha(): Int {
            val que = (now - initTime - marginTime).toInt() / showAnswerLength
            val timeInPhase = (now - initTime - marginTime - (showAnswerLength * que).toLong()).toInt()

            if (!isFirstFlash) {
                return 0
            } else {
                if (timeInPhase < showAnswerLength * 0.2) {
                    return (255 - 255 * timeInPhase / (showAnswerLength * 0.2)).toInt()
                } else if (timeInPhase < showAnswerLength * 0.7) {
                    return 0
                } else {
                    return (255 * (timeInPhase - timeInPhase * 0.7) / (showAnswerLength - showAnswerLength * 0.7)).toInt()
                }
            }
        }

        fun setGrainAlpha(time: Long) {
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
                for (y in 0..h - 1) {
                    for (x in 0..w - 1) {
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
            if (now - time > tol) {
                resetThrough()
                var result = ((now - time - tol.toLong()) / 2f).toInt()
                if (result > 255) {
                    result = 255
                }
                return result
            } else {
                return 0
            }
        }

        internal inner class Particle(var x0: Float, var y0: Float) {
            var grain = ArrayList<Grain>()
            var phase = 0
            var moveFrames: Long = 400
            var initFrames: Long = 0
            var v = 0.15

            init {
                initFrames = System.currentTimeMillis()
                grain.add(Grain(x0, y0))
                grain.add(Grain(x0, y0))
                grain.add(Grain(x0, y0))
            }

            fun move() {
                val diffFrames = System.currentTimeMillis() - initFrames
                if (diffFrames > moveFrames || isReleased || !isStartGame) phase = 1

                if (phase == 0) {
                    val param = (moveFrames - diffFrames) / (moveFrames * 1.0f)
                    grain[0].x = grain[0].step0[0] + grain[0].diff[0] * param
                    grain[0].y = grain[0].step0[1] + grain[0].diff[1] * param
                    grain[1].x = grain[1].step0[0] + grain[1].diff[0] * param
                    grain[1].y = grain[1].step0[1] + grain[1].diff[1] * param
                    grain[2].x = grain[2].step0[0] + grain[2].diff[0] * param
                    grain[2].y = grain[2].step0[1] + grain[2].diff[1] * param
                }
                if (phase == 1) {
                    var param = Math.cos(grain[0].a0)
                    grain[0].x += (Math.cos(grain[0].a1) * grain[0].circleR * param).toFloat()
                    grain[0].y += (Math.sin(grain[0].a1) * grain[0].circleR * param).toFloat()
                    grain[0].a0 += v
                    param = Math.cos(grain[1].a0)
                    grain[1].x += (Math.cos(grain[1].a1) * grain[1].circleR * param).toFloat()
                    grain[1].y += (Math.sin(grain[1].a1) * grain[1].circleR * param).toFloat()
                    grain[1].a0 += v
                    param = Math.cos(grain[2].a0)
                    grain[2].x += (Math.cos(grain[2].a1) * grain[2].circleR * param).toFloat()
                    grain[2].y += (Math.sin(grain[2].a1) * grain[2].circleR * param).toFloat()
                    grain[2].a0 += v
                }
                draw()
            }

            private fun draw() {
                for (gr in grain) {
                    c?.drawBitmap(scaledGrain, gr.x - grainR, gr.y - grainR, p)
                }
            }
        }

        internal inner class Grain(x: Float, y: Float) {
            var x: Float = 0.toFloat()
            var y: Float = 0.toFloat()
            var origin = FloatArray(2)
            var step0 = FloatArray(2)
            var step1 = FloatArray(2)
            var diff = FloatArray(2)
            var a0 = Math.random() * Math.PI * 2.0
            var a1 = Math.random() * Math.PI * 2.0
            var circleR = Math.random() * 0.5 + 0.7

            init {
                origin[0] = x
                origin[1] = y

                var blurR = Math.random() * offsetX.toDouble() * 0.05
                var blurA = Math.random() * Math.PI * 2.0
                step0[0] = origin[0] + (blurR * Math.cos(blurA)).toFloat()
                step0[1] = origin[1] + (blurR * Math.sin(blurA)).toFloat()


                blurR = offsetX * 0.2 + Math.random() * offsetX.toDouble() * 0.05
                blurA = Math.random() * Math.PI * 2.0
                step1[0] = origin[0] + (blurR * Math.cos(blurA)).toFloat()
                step1[1] = origin[1] + (blurR * Math.sin(blurA)).toFloat()

                diff[0] = step1[0] - step0[0]
                diff[1] = step1[1] - step0[1]

                if (isReleased || !isStartGame) {
                    this.x = step0[0]
                    this.y = step0[1]
                } else {
                    this.x = step1[0]
                    this.y = step1[1]
                }
            }
        }

        fun putParticles(throughList: ThroughList) {
            val interval = 25 * scale
            val length = FloatArray(throughList.dots.size - 1)
            for (i in 1..throughList.dots.size - 1) {
                val dotI0 = dots[throughList.dots[i]]
                val dotI1 = dots[throughList.dots[i - 1]]
                if (dotI0 != null && dotI1 != null) {
                    length[i - 1] = Math.sqrt(Math.pow((dotI1.x - dotI0.x).toDouble(), 2.0) + Math.pow((dotI1.y - dotI0.y).toDouble(), 2.0)).toFloat()
                }
            }
            Locus.clear()
            for (i in length.indices) {
                val dotI0 = dots[throughList.dots[i]]
                val dotI1 = dots[throughList.dots[i + 1]]
                if (dotI0 != null && dotI1 != null) {
                    val unitV = floatArrayOf((dotI1.x - dotI0.x) / length[i], (dotI1.y - dotI0.y) / length[i])
                    var x = dotI0.x
                    var y = dotI0.y

                    val sumLength = floatArrayOf(0f, 0f)
                    while (sumLength[0] <= Math.abs(dotI1.x - dotI0.x) && sumLength[1] <= Math.abs(dotI1.y - dotI0.y)) {
                        Locus.add(Particle(x, y))
                        x += unitV[0] * interval
                        y += unitV[1] * interval
                        sumLength[0] += Math.abs(dotI1.x - dotI0.x) * interval / length[i]
                        sumLength[1] += Math.abs(dotI1.y - dotI0.y) * interval / length[i]
                    }
                }
            }
        }

        var lastTime: Long = -1
        var interTime: Long = -1
        var frames = 0
        var sumTimes = 0
        var fps = -1f
        fun showFPS() {
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
                p.textSize = 30 * scale
                p.textAlign = Paint.Align.LEFT
                c?.drawText("FPS:" + "%.2f".format(fps), 0f, offsetY * 2 - 120 * scale, p)
            }
        }

        fun getCorrectStrings(c: Cursor): ArrayList<String> {
            val strings = ArrayList(Arrays.asList(*c.getString(c.getColumnIndex("sequence")).split(",".toRegex()).toTypedArray()))
            val correctStrings = if (c.isNull(c.getColumnIndex("correctSeq"))) null else ArrayList(Arrays.asList(*c.getString(c.getColumnIndex("correctSeq")).split(",".toRegex()).toTypedArray()))

            if (correctStrings != null) {
                val tStrings = ArrayList<String>()
                for (i in correctStrings.indices) {
                    if (correctStrings[i] == "") {
                        tStrings.add(strings[i])
                    } else {
                        tStrings.add(correctStrings[i])
                    }
                }
                return tStrings
            } else {
                return strings
            }
        }

        fun showButton() {
            val nextButtonWidth = (if (isStartGame && doShow) 200 else 150) * scale
            val retryButtonWidth = 170 * scale
            val buttonHeight = 90 * scale
            val margin = 20 * scale
            nextButtonPoint[0] = Point((offsetX * 2 - nextButtonWidth - margin).toInt(), (offsetY * 2 - buttonHeight - margin).toInt())
            nextButtonPoint[1] = Point((offsetX * 2 - margin).toInt(), (offsetY * 2 - margin).toInt())
            retryButtonPoint[0] = Point((margin).toInt(), (offsetY * 2 - buttonHeight - margin).toInt())
            retryButtonPoint[1] = Point((margin + retryButtonWidth).toInt(), (offsetY * 2 - margin).toInt())

            p.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
            p.textAlign = Paint.Align.CENTER
            p.textSize = 40 * scale
            val dNext: Drawable
            val dRetry: Drawable
            if (isOnNext) {
                dNext = if (version >= 23) resources.getDrawable(R.drawable.button1, null) else resources.getDrawable(R.drawable.button1)
            } else {
                dNext = if (version >= 23) resources.getDrawable(R.drawable.button0, null) else resources.getDrawable(R.drawable.button0)
            }
            dNext.setBounds(nextButtonPoint[0]?.x ?: 0, nextButtonPoint[0]?.y ?: 0, nextButtonPoint[1]?.x ?: 0, nextButtonPoint[1]?.y ?: 0)
            dNext.draw(c)
            if (isStartGame && doShow) {
                c?.drawText("BYPASS", (nextButtonPoint[0]?.x ?: 0) + nextButtonWidth / 2, (nextButtonPoint[1]?.y ?: 0) - 30 * scale, p)
            } else {
                c?.drawText("NEXT", (nextButtonPoint[0]?.x ?: 0) + nextButtonWidth / 2, (nextButtonPoint[1]?.y ?: 0) - 30 * scale, p)
            }
            if (isEndGame) {
                if (isOnRetry) {
                    dRetry = if (version >= 23) resources.getDrawable(R.drawable.button1, null) else resources.getDrawable(R.drawable.button1)
                } else {
                    dRetry = if (version >= 23) resources.getDrawable(R.drawable.button0, null) else resources.getDrawable(R.drawable.button0)
                }
                dRetry.setBounds(retryButtonPoint[0]?.x ?: 0, retryButtonPoint[0]?.y ?: 0, retryButtonPoint[1]?.x ?: 0, retryButtonPoint[1]?.y ?: 0)
                dRetry.draw(c)
                c?.drawText("RETRY", (retryButtonPoint[0]?.x ?: 0) + retryButtonWidth / 2, (retryButtonPoint[1]?.y ?: 0) - 30 * scale, p)
            }
        }

        fun showCount() {
            p.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
            p.textSize = 40f
            p.textAlign = Paint.Align.RIGHT
            val x = (offsetX * 2.0 - 20.0 * scale).toFloat()
            val y = (offsetY * 2.0 - 120.0 * scale).toFloat()

            c?.drawText("HACK:" + viewCount, x, y, p)
        }

        fun makeHexagon(origin: PointF, r: Float): Path {
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
        fun showTime(currentTime: Long) {
            val tag = "showTime"
            leftTime = (defTime - ((if (isEndGame) holdTime else currentTime) - initTime)) / 10

            if (leftTime <= 0 && isFirstTimeUp) {
                for (i in 0..qTotal - 1) {
                    Log.v(tag, "q[" + i + "]:" + judgeLocus(answerThroughList[i]!!, throughList[i]!!))
                }
                holdTime = now
                isEndGame = true
                isFirstTimeUp = false
            }

            if (doShow) {
                p.textSize = 60 * scale
                p.color = Color.rgb(220, 190, 50)
                val dispTime = if (isEndGame) upTime else leftTime

                p.textAlign = Paint.Align.RIGHT
                c?.drawText("%02d".format(dispTime / 100), offsetX - 3, offsetY / 3, p)
                p.textAlign = Paint.Align.CENTER
                c?.drawText(":", offsetX, offsetY / 3, p)
                p.textAlign = Paint.Align.LEFT
                c?.drawText("%02d".format(dispTime % 100), offsetX + 3, offsetY / 3, p)

                val barWidth = (offsetX * 0.7 / defTime).toFloat() * leftTime.toFloat() * 10f
                p.style = Paint.Style.FILL
                c?.drawRect(offsetX - barWidth, (offsetY / 2.7).toFloat(), offsetX + barWidth, (offsetY / 2.55).toFloat(), p)
            } else {
                p.textSize = 70 * scale
                p.color = Color.WHITE
                p.textAlign = Paint.Align.RIGHT
                c?.drawText("%02d".format(upTime / 100), offsetX - 5, offsetY / 9, p)
                p.textAlign = Paint.Align.CENTER
                c?.drawText(":", offsetX, offsetY / 9, p)
                p.textAlign = Paint.Align.LEFT
                c?.drawText("%02d".format(upTime % 100), offsetX + 5, offsetY / 9, p)
            }
        }

        fun showQueNumber(currentTime: Long, marginTime: Long, r: Int, g: Int, b: Int) {
            val hexRadius = offsetX / 10
            val hexMargin = 5f
            val totalMargin = hexMargin * (qTotal - 1)
            val width = (qTotal - 1) * (offsetX / 5)
            var x: Float
            var y: Float
            val arrayNormal = intArrayOf(Color.argb(140, r, g, b), Color.argb(70, r, g, b), Color.argb(45, r, g, b), Color.argb(40, r, g, b), Color.argb(45, r, g, b), Color.argb(70, r, g, b), Color.argb(140, r, g, b))
            val arrayStrong = intArrayOf(Color.argb(255, r, g, b), Color.argb(130, r, g, b), Color.argb(85, r, g, b), Color.argb(75, r, g, b), Color.argb(85, r, g, b), Color.argb(130, r, g, b), Color.argb(255, r, g, b))
            val positions = floatArrayOf(0f, 0.15f, 0.35f, 0.5f, 0.65f, 0.85f, 1f)

            for (i in 0..qTotal - 1) {
                x = offsetX - (width / 2 + totalMargin) + i.toFloat() * (hexRadius + hexMargin) * 2f
                y = (offsetY / 7.5).toFloat()
                val origin = PointF(x, y)
                val lgNormal = LinearGradient(x, y - hexRadius, x, y + hexRadius, arrayNormal, positions, Shader.TileMode.CLAMP)
                val lgStrong = LinearGradient(x, y - hexRadius, x, y + hexRadius, arrayStrong, positions, Shader.TileMode.CLAMP)

                p.color = Color.BLACK
                if (isStartGame) {
                    when (i) {
                        in 0..qNum - 1 -> {
                            p.setShader(lgNormal)
                            p.style = Paint.Style.FILL
                            c?.drawPath(makeHexagon(origin, hexRadius), p)
                            p.setShader(null)

                            p.strokeJoin = Paint.Join.BEVEL
                            p.color = Color.argb(140, r, g, b)
                            p.strokeWidth = 2f
                            p.style = Paint.Style.STROKE
                            c?.drawPath(makeHexagon(origin, hexRadius), p)
                            p.strokeWidth = 0f
                        }
                        qNum -> {
                            if ((isReleased && throughList[qTotal - 1]?.dots?.size ?: 0 > 0)) {
                                p.setShader(lgNormal)
                                p.style = Paint.Style.FILL
                                c?.drawPath(makeHexagon(origin, hexRadius), p)
                                p.setShader(null)

                                p.strokeJoin = Paint.Join.BEVEL
                                p.color = Color.argb(140, r, g, b)
                                p.strokeWidth = 2f
                                p.style = Paint.Style.STROKE
                                c?.drawPath(makeHexagon(origin, hexRadius), p)
                                p.strokeWidth = 0f
                            } else {
                                p.setShader(lgStrong)
                                p.style = Paint.Style.FILL
                                c?.drawPath(makeHexagon(origin, hexRadius), p)
                                p.setShader(null)

                                p.strokeJoin = Paint.Join.BEVEL
                                p.color = Color.rgb(r, g, b)
                                p.strokeWidth = 2f
                                p.style = Paint.Style.STROKE
                                c?.drawPath(makeHexagon(origin, hexRadius), p)
                                p.strokeWidth = 0f
                            }
                        }
                        else -> {
                            p.color = Color.BLACK
                            p.style = Paint.Style.FILL
                            c?.drawPath(makeHexagon(origin, hexRadius), p)

                            p.strokeJoin = Paint.Join.BEVEL
                            p.color = Color.argb(80, r, g, b)
                            p.strokeWidth = 2f
                            p.style = Paint.Style.STROKE
                            c?.drawPath(makeHexagon(origin, hexRadius), p)
                            p.strokeWidth = 0f
                        }
                    }
                } else {
                    if (i.toLong() == (currentTime - marginTime) / showAnswerLength && currentTime > marginTime) {
                        p.setShader(lgStrong)
                        p.style = Paint.Style.FILL
                        c?.drawPath(makeHexagon(origin, hexRadius), p)
                        p.setShader(null)

                        p.strokeJoin = Paint.Join.BEVEL
                        p.color = Color.rgb(r, g, b)
                        p.strokeWidth = 2f
                        p.style = Paint.Style.STROKE
                        c?.drawPath(makeHexagon(origin, hexRadius), p)
                        p.strokeWidth = 0f
                    } else {
                        p.color = Color.BLACK
                        p.style = Paint.Style.FILL
                        c?.drawPath(makeHexagon(origin, hexRadius), p)

                        p.strokeJoin = Paint.Join.BEVEL
                        p.color = Color.argb(140, r, g, b)
                        p.strokeWidth = 2f
                        p.style = Paint.Style.STROKE
                        c?.drawPath(makeHexagon(origin, hexRadius), p)
                        p.strokeWidth = 0f
                    }
                }
            }
        }

        var preQue = -1
        var initFlashTime: Long = 0
        var isFirstFlash = true
        fun showAnswer(initTime: Long, currentTime: Long) {
            var que = -1
            val diffTIme = currentTime - initTime - marginTime

            if (diffTIme >= 0) {
                que = diffTIme.toInt() / showAnswerLength
            }
            if (que < qTotal) {
                if (que >= 0) {
                    if (gameMode == 0 || gameMode == 1) {
                        p.color = Color.WHITE
                        p.textSize = 80 * scale
                        p.textAlign = Paint.Align.CENTER
                        c?.drawText(correctStr[que], offsetX, offsetY / 3, p)
                    }
                    if (preQue != que && (gameMode == 0 || gameMode == 2)) {
                        putParticles(answerThroughList[que]!!)
                    }
                }
            } else {
                if (isFirstFlash) {
                    resetLocus()
                    initFlashTime = System.currentTimeMillis()
                    isFirstFlash = false
                }
                showFlash(initFlashTime, currentTime)
            }
            preQue = que
        }

        fun showFlash(initTime: Long, currentTime: Long) {
            var que: Int
            val interval = 1000
            val margin = interval / 800
            val diffTime = (currentTime - initTime).toInt()
            var alpha = 255

            que = diffTime / interval
            if (diffTime > interval * 2.5) {
                que++
            }

            if (que == 0) {
                if (diffTime < margin) {
                    alpha = 150 * diffTime / margin
                } else {
                    alpha = 150 - 150 * diffTime / interval
                }
            }
            if (que == 1) {
                if (diffTime < margin) {
                    alpha = 200 * diffTime / margin
                } else {
                    alpha = 200 - 200 * diffTime / interval
                }
            }
            p.color = Color.argb(alpha, 220, 175, 50)
            if (que == 2) {
                if (diffTime < margin) {
                    alpha = 255 * diffTime / margin
                } else {
                    alpha = 255
                }
                p.color = Color.argb(alpha, 255, 255, 255)
            }
            p.style = Paint.Style.FILL
            c?.drawRect(0.0f, 0.0f, offsetX * 2, offsetY * 2, p)
            if (que > 2) {
                this.initTime = System.currentTimeMillis()
                isStartGame = true
            }
        }

        //var isFirstShowResult = true
        fun showResult(margin: Long, initTime: Long, currentTime: Long) {
            val tag = "showResult"

            if (currentTime > initTime + margin) {
                showTime(now)

                val blue = Color.rgb(2, 255, 197)
                val red = Color.RED
                var drawColor: Int
                var correctNum = 0
                for (i in 0..qTotal - 1) {
                    val answerPath = Path()
                    val hexaRadius = offsetX / 8
                    val hexaMargin = 10 * scale
                    val totalMargin = hexaMargin * (qTotal - 1)
                    val height = (qTotal - 1) * (offsetX / 5)
                    val x = offsetX / 6
                    val y = offsetY * 2 / 3 - (height / 2 + totalMargin) + i.toFloat() * (hexaRadius + hexaMargin) * 2f
                    val giveOrigin = PointF(x, y)
                    if (judgeLocus(answerThroughList[i]!!, throughList[i]!!)) {
                        drawColor = blue
                        correctNum++
                    } else {
                        drawColor = red
                    }

                    p.color = Color.argb(80, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor))
                    p.style = Paint.Style.FILL
                    c?.drawPath(makeHexagon(giveOrigin, hexaRadius), p)

                    p.color = Color.argb(255, Color.red(drawColor), Color.green(drawColor), Color.blue(drawColor))
                    p.style = Paint.Style.STROKE
                    c?.drawPath(makeHexagon(giveOrigin, hexaRadius), p)

                    for (j in answerThroughList[i]!!.dots.indices) {
                        if (j == 0) {
                            answerPath.moveTo(x - hexaRadius + dots[answerThroughList[i]!!.dots[j]]!!.x / 8, y + (dots[answerThroughList[i]!!.dots[j]]!!.y - offsetY * 1.2).toFloat() / 8)
                        } else {
                            answerPath.lineTo(x - hexaRadius + dots[answerThroughList[i]!!.dots[j]]!!.x / 8, y + (dots[answerThroughList[i]!!.dots[j]]!!.y - offsetY * 1.2).toFloat() / 8)
                        }
                    }
                    p.strokeWidth = 3 * scale
                    c?.drawPath(answerPath, p)

                    p.style = Paint.Style.FILL
                    p.strokeWidth = 1f
                    p.textSize = 70 * scale
                    p.textAlign = Paint.Align.LEFT
                    c?.drawText(correctStr[i], x * 2, giveOrigin.y + 25 * scale, p)
                    p.textSize = 50 * scale
                    p.textAlign = Paint.Align.RIGHT
                    p.color = Color.WHITE
                    if (passTime[i] > -1) {
                        c?.drawText("${passTime[i] / 100}:${passTime[i] % 100}", offsetX * 2 - 5 * scale, giveOrigin.y + 20 * scale, p)
                    }
                }
                /*if (isFirstShowResult) {
                    Cursor cursor = db.query(DBHelper.TABLE_NAME3, null, "id = " + (randomVal + 1), null, null, null, null, null);
                    cursor.moveToFirst();
                    int totalTimes = cursor.getInt(cursor.getColumnIndex("total_times"));
                    ContentValues contentValues = new ContentValues();
                    if (correctNum == qTotal) {
                        contentValues.put("correct_times", totalTimes > -1 ? cursor.getInt(cursor.getColumnIndex("correct_times")) + 1 : 1);
                        contentValues.put("total_times", totalTimes > -1 ? totalTimes + 1 : 1);
                    } else {
                        contentValues.put("correct_times", totalTimes > -1 ? cursor.getInt(cursor.getColumnIndex("correct_times")) : 0);
                        contentValues.put("total_times", totalTimes > -1 ? totalTimes + 1 : 1);
                    }
                    cursor.close();
                    db.update(DBHelper.TABLE_NAME3, contentValues, "id = " + (randomVal + 1), null);
                    isFirstShowResult = false;
                }*/

                p.color = if (version >= 23) resources.getColor(R.color.button_text, null) else resources.getColor(R.color.button_text)
                p.textSize = 40 * scale
                p.textAlign = Paint.Align.CENTER
                c?.drawText(getText(R.string.bonus_hack).toString(), offsetX, offsetY * 4 / 3, p)
                c?.drawText(getText(R.string.bonus_speed).toString(), offsetX, offsetY * 5 / 3, p)
                p.color = Color.WHITE
                p.textSize = 120 * scale
                c?.drawText("${Math.round((difficulty[level].bonus).toDouble() * correctNum / qTotal)}%", offsetX, offsetY * 5 / 3 - 80 * scale, p)
                if (correctNum == qTotal) {
                    c?.drawText("${Math.round((upTime * 1000).toDouble() / defTime)}%", offsetX, offsetY * 2 - 80 * scale, p)
                } else {
                    c?.drawText("0%", offsetX, offsetY * 2 - 80 * scale, p)
                }
            }
        }

        fun normalizePaths(paths: ArrayList<IntArray>): ArrayList<IntArray> {
            val returnPaths = ArrayList<IntArray>()
            for (i in paths.indices) {
                var match = 0
                val srcPath = paths[i]
                for (j in i + 1..paths.size - 1) {
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

        fun judgeLocus(answer: ThroughList, through: ThroughList): Boolean {
            val answerPaths = ArrayList<IntArray>()
            var passedPaths = ArrayList<IntArray>()

            for (i in 0..answer.dots.size - 1 - 1) {
                val path = intArrayOf(answer.dots[i], answer.dots[i + 1])
                answerPaths.add(path)
            }
            for (i in 0..through.dots.size - 1 - 1) {
                val path = intArrayOf(through.dots[i], through.dots[i + 1])
                passedPaths.add(path)
            }
            passedPaths = normalizePaths(passedPaths)

            if (answerPaths.size == passedPaths.size) {
                val clearFrags = BooleanArray(answerPaths.size)
                for (i in answerPaths.indices) {
                    for (path in passedPaths) {
                        val tempPaths = intArrayOf(path[1], path[0])
                        if (Arrays.equals(answerPaths[i], path) || Arrays.equals(answerPaths[i], tempPaths)) {
                            clearFrags[i] = true
                        }
                    }
                }
                var clearC = 0
                for (flag in clearFrags) {
                    if (flag) {
                        clearC++
                    }
                }
                return (clearC == answerPaths.size)
            } else {
                return false
            }
        }

        fun setLocusStart(x: Float, y: Float, doCD: Boolean) {
            Locus.add(Particle(x, y))

            if (doCD) {
                setCollision(x, y, x, y)
            }
            locusPath.moveTo(x, y)
        }

        fun setLocus(x: Float, y: Float, doCD: Boolean) {
            Locus.add(Particle(x, y))

            if (doCD) {
                setCollision(x, y, Locus[Locus.size - 2].x0, Locus[Locus.size - 2].y0)
            }
            locusPath.lineTo(x, y)
        }

        fun setCollision(x0: Float, y0: Float, x1: Float, y1: Float) {
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
            if (collisionDot != -1 && (throughList[qNum]!!.dots.size < 1 || throughList[qNum]!!.dots[throughList[qNum]!!.dots.size - 1] !== collisionDot)) {
                throughList[qNum]?.dots?.add(collisionDot)
                if (doVibrate) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(30)
                }
                previousDot = collisionDot
            }
        }

        fun resetLocus() {
            locusPath.reset()
            Locus.clear()
        }

        fun resetThrough() {
            for (i in 0..10) {
                isThrough[i] = false
            }
        }

        var downX = 0f
        var downY = 0f
        var memX = 0f
        var memY = 0f
        var isReleased = false
        var isFirstPress = true
        var isOnNext = false
        var isOnRetry = false
        var releaseTime: Long = -1
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val tag = "onTouchEvent"

            val lim = 25 * scale
            val upX: Float
            val upY: Float
            when (event.action) {
                MotionEvent.ACTION_DOWN //タッチ
                -> {
                    downX = event.x
                    downY = event.y
                    isOnNext = nextButtonPoint[0]!!.x <= downX && downX <= nextButtonPoint[1]!!.x && nextButtonPoint[0]!!.y <= downY && downY <= nextButtonPoint[1]!!.y
                    isOnRetry = isEndGame && retryButtonPoint[0]!!.x <= downX && downX <= retryButtonPoint[1]!!.x && retryButtonPoint[0]!!.y <= downY && downY <= retryButtonPoint[1]!!.y
                    releaseTime = -1
                    if (!isOnNext && isStartGame && !isEndGame) {
                        if (isReleased) {
                            resetLocus()
                            resetThrough()
                            isReleased = false
                        }
                        setLocusStart(downX, downY, true)
                        memX = downX
                        memY = downY
                    }
                }
                MotionEvent.ACTION_MOVE //スワイプ
                -> {
                    val currentX = event.x
                    val currentY = event.y
                    isOnNext = nextButtonPoint[0]!!.x <= currentX && currentX <= nextButtonPoint[1]!!.x && nextButtonPoint[0]!!.y <= currentY && currentY <= nextButtonPoint[1]!!.y
                    isOnRetry = isEndGame && retryButtonPoint[0]!!.x <= currentX && currentX <= retryButtonPoint[1]!!.x && retryButtonPoint[0]!!.y <= currentY && currentY <= retryButtonPoint[1]!!.y
                    if (!isOnNext && isStartGame && !isEndGame) {
                        if (currentX + lim < memX || memX + lim < currentX || currentY + lim < memY || memY + lim < currentY) {
                            if (Locus.size == 0) {
                                setLocusStart(currentX, currentY, true)
                            }
                            setLocus(currentX, currentY, true)
                            memX = currentX
                            memY = currentY
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL //リリース
                -> {
                    upX = event.x
                    upY = event.y
                    isOnNext = nextButtonPoint[0]!!.x <= downX && downX <= nextButtonPoint[1]!!.x && nextButtonPoint[0]!!.y <= downY && downY <= nextButtonPoint[1]!!.y &&
                            nextButtonPoint[0]!!.x <= upX && upX <= nextButtonPoint[1]!!.x && nextButtonPoint[0]!!.y <= upY && upY <= nextButtonPoint[1]!!.y
                    isOnRetry = isEndGame &&
                            retryButtonPoint[0]!!.x <= downX && downX <= retryButtonPoint[1]!!.x && retryButtonPoint[0]!!.y <= downY && downY <= retryButtonPoint[1]!!.y &&
                            retryButtonPoint[0]!!.x <= upX && upX <= retryButtonPoint[1]!!.x && retryButtonPoint[0]!!.y <= upY && upY <= retryButtonPoint[1]!!.y
                    releaseTime = now
                    if (!isOnNext && !isOnRetry && isStartGame && !isEndGame) {
                        isReleased = true
                        var tpassTime = (now - initTime) / 10
                        for (i in 0..qNum - 1) {
                            tpassTime -= passTime[i]
                        }
                        passTime[qNum] = tpassTime
                        var list = ""
                        for (throughDot in throughList[qNum]!!.dots) {
                            list += "$throughDot,"
                        }
                        Log.v(tag, "throughList:$list")
                        resetLocus()
                        if (throughList[qNum]?.dots?.size ?: 0 > 0) {
                            putParticles(throughList[qNum]!!)
                        }

                        if (qTotal - 1 > qNum) {
                            qNum++
                        } else {
                            holdTime = now
                            upTime = (defTime - (holdTime - initTime)) / 10
                            isEndGame = true
                            for (i in 0..qTotal - 1) {
                                Log.v(tag, "q[" + i + "]:" + judgeLocus(answerThroughList[i]!!, throughList[i]!!))
                            }
                        }
                    }
                    if (isOnNext) {
                        if (!isStartGame) {
                            startActivity(Intent(this@MyActivity, MyActivity::class.java))
                        } else if (doShow) {
                            if (isFirstPress) {
                                now = initTime + defTime
                                holdTime = now
                                pressButtonTime = System.currentTimeMillis()
                                upTime = (defTime - (pressButtonTime - initTime)) / 10
                                isPressedButton = true
                                isFirstPress = false
                            }
                        } else {
                            startActivity(Intent(this@MyActivity, MyActivity::class.java))
                        }
                    }
                    if (isOnRetry) {
                        val intent = Intent(this@MyActivity, MyActivity::class.java)
                        intent.putExtra("isRetry", true)
                        intent.putExtra("retryLevel", level)
                        intent.putExtra("retryValue", randomVal)
                        startActivity(intent)
                    }
                    isOnNext = false
                    isOnRetry = false
                }
            }
            return true
        }
    }
}