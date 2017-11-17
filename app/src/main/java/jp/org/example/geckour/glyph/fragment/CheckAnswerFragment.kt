package jp.org.example.geckour.glyph.fragment

import android.content.res.ColorStateList
import android.databinding.DataBindingUtil
import android.graphics.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import io.realm.Realm
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.coda
import jp.org.example.geckour.glyph.App.Companion.version
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.MainActivity
import jp.org.example.geckour.glyph.activity.MainActivity.Companion.hacks
import jp.org.example.geckour.glyph.databinding.FragmentCheckAnswerBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.fragment.model.Result
import jp.org.example.geckour.glyph.util.getDifficulty
import jp.org.example.geckour.glyph.util.toTimeStringPair

class CheckAnswerFragment: Fragment() {

    companion object {
        val tag: String = CheckAnswerFragment::class.java.simpleName

        private val ARGS_RESULTS = "results"
        private val ARGS_ALLOWABLE_TIME = "allowableTime"

        fun newInstance(results: List<Result>, allowableTime: Long): CheckAnswerFragment =
                CheckAnswerFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(ARGS_RESULTS, ArrayList(results))
                        putLong(ARGS_ALLOWABLE_TIME, allowableTime)
                    }
                }
    }

    private lateinit var binding: FragmentCheckAnswerBinding
    private lateinit var realm: Realm

    private val mainActivity: MainActivity by lazy { activity as MainActivity }

    private val results: ArrayList<Result> = ArrayList()
    private var allowableTime: Long = -1L

    private var showCount: Boolean = false

    private val shaperImg: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal)
    }

    private val dotsPoint: Array<PointF> = Array(11) {
        val c = when (it) {
            1 -> 1
            2 -> 3
            3 -> 4
            4 -> 6
            in 5..10 -> it
            else -> 0
        }

        val uAngle = Math.PI / 3.0

        if (it == 0) PointF(0f, 0f)
        else {
            PointF(
                    (Math.cos(uAngle * (c - 0.5)) * (if (it < 5) 0.5 else 1.0)).toFloat(),
                    (Math.sin(uAngle * (c - 0.5)) * (if (it < 5) 0.5 else 1.0)).toFloat()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()

        results.addAll(arguments.getParcelableArrayList(ARGS_RESULTS))
        allowableTime = arguments.getLong(ARGS_ALLOWABLE_TIME)

        recordScore()

        val t: Tracker? = (activity.application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_check_answer, container, false)

        showCount = App.sp.getBoolean("showCountView", false)

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLeftButton("RETRY") {
            mainActivity.onRetry()
        }
        setRightButton("NEXT") {
            mainActivity.onNext()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        injectResults()
    }

    private fun recordScore() {
        mainActivity.level?.getDifficulty()?.let { difficulty ->
            if (difficulty == 1) {
                val shaper = realm.where(Shaper::class.java)
                        .equalTo("id", mainActivity.sequenceId)
                        .findFirst()

                shaper?.let { s ->
                    realm.executeTransaction {
                        s.examCount++
                        if (results.first().correct) s.correctCount++
                    }
                }
            } else {
                val sequence = realm.where(Sequence::class.java)
                        .equalTo("id", mainActivity.sequenceId)
                        .findFirst()

                sequence?.let { seq ->
                    realm.executeTransaction {
                        seq.examCount++
                        if (results.count { it.correct } == results.size) seq.correctCount++
                    }
                }
            }
        }
    }

    private fun injectResults() {
        results.forEachIndexed { i, result ->
            val shaper = realm.where(Shaper::class.java).equalTo("id", result.shaperId).findFirst()
            val tintColor = if (result.correct) Color.rgb(2, 255, 197) else Color.RED

            shaper?.let {
                when (i) {
                    0 -> {
                        binding.shaper1.apply {
                            setImageBitmap(getShaperImage(it))
                            if (version > 20) imageTintList = ColorStateList.valueOf(tintColor)
                            else setColorFilter(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper1Name.apply {
                            text = it.name
                            setTextColor(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper1Time.apply {
                            text = result.spentTime.toTimeStringPair().let { "${it.first}:${it.second}" }
                            visibility = View.VISIBLE
                        }
                    }

                    1 -> {
                        binding.shaper2.apply {
                            setImageBitmap(getShaperImage(it))
                            if (version > 20) imageTintList = ColorStateList.valueOf(tintColor)
                            else setColorFilter(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper2Name.apply {
                            text = it.name
                            setTextColor(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper2Time.apply {
                            text = result.spentTime.toTimeStringPair().let { "${it.first}:${it.second}" }
                            visibility = View.VISIBLE
                        }
                    }

                    2 -> {
                        binding.shaper3.apply {
                            setImageBitmap(getShaperImage(it))
                            if (version > 20) imageTintList = ColorStateList.valueOf(tintColor)
                            else setColorFilter(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper3Name.apply {
                            text = it.name
                            setTextColor(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper3Time.apply {
                            text = result.spentTime.toTimeStringPair().let { "${it.first}:${it.second}" }
                            visibility = View.VISIBLE
                        }
                    }

                    3 -> {
                        binding.shaper4.apply {
                            setImageBitmap(getShaperImage(it))
                            if (version > 20) imageTintList = ColorStateList.valueOf(tintColor)
                            else setColorFilter(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper4Name.apply {
                            text = it.name
                            setTextColor(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper4Time.apply {
                            text = result.spentTime.toTimeStringPair().let { "${it.first}:${it.second}" }
                            visibility = View.VISIBLE
                        }
                    }

                    4 -> {
                        binding.shaper5.apply {
                            setImageBitmap(getShaperImage(it))
                            if (version > 20) imageTintList = ColorStateList.valueOf(tintColor)
                            else setColorFilter(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper5Name.apply {
                            text = it.name
                            setTextColor(tintColor)
                            visibility = View.VISIBLE
                        }
                        binding.shaper5Time.apply {
                            text = result.spentTime.toTimeStringPair().let { "${it.first}:${it.second}" }
                            visibility = View.VISIBLE
                        }
                    }

                    else -> {}
                }
            }
        }

        if (showCount) {
            binding.countHackValue = hacks
            binding.countHack.visibility = View.VISIBLE
        }
        binding.bonusHackValue = calcHackBonus()
        binding.bonusSpeedValue = calcSpeedBonus()
    }

    private fun getShaperImage(shaper: Shaper): Bitmap {
        val copyImg = shaperImg.copy(shaperImg.config, true)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 30f * mainActivity.scale
            strokeJoin = Paint.Join.BEVEL
        }

        Canvas(copyImg).drawPath(
                shaper.dots
                        .map { dotsPoint[it] }
                        .let {
                            Path().apply {
                                it.forEachIndexed { i, pointF ->
                                    if (i < 1) moveTo((pointF.x * 0.4f + 0.5f) * copyImg.width, (pointF.y * 0.4f + 0.5f) * copyImg.height)
                                    else lineTo((pointF.x * 0.4f + 0.5f) * copyImg.width, (pointF.y * 0.4f + 0.5f) * copyImg.height)
                                }
                                if (shaper.dots.first() == shaper.dots.last()) close()
                            }
                        }, paint
        )

        return copyImg
    }

    private fun calcHackBonus(): Int =
            Math.round(when (results.size) {
                1 -> 38
                2 -> 60
                3 -> 85
                4 -> 120
                5 -> 162
                else -> 0
            } * results.count { it.correct }.toDouble() / results.size).toInt()

    private fun calcSpeedBonus(): Int =
            if (results.count { it.correct } == results.size) Math.round((allowableTime - results.sumByDouble { it.spentTime.toDouble() }) * 100 / allowableTime).toInt()
            else 0

    private fun setRightButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonRight.apply {
            text = buttonText
            setOnClickListener { predicate(it) }
            visibility = View.VISIBLE
            typeface = coda
        }
    }

    private fun hideRightButton() {
        binding.buttonRight.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }

    private fun setLeftButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonLeft.apply {
            text = buttonText
            setOnClickListener { predicate(it) }
            visibility = View.VISIBLE
            typeface = coda
        }
    }

    private fun hideLeftButton() {
        binding.buttonLeft.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }
}