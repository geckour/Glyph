package jp.org.example.geckour.glyph.ui

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.realm.Realm
import jp.org.example.geckour.glyph.App.Companion.moshi
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.FragmentCheckAnswerBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.ui.model.Result
import jp.org.example.geckour.glyph.util.*

class CheckAnswerFragment : Fragment() {

    companion object {
        val tag: String = CheckAnswerFragment::class.java.simpleName

        private const val ARGS_RESULT = "result"
        private const val ARGS_ALLOWABLE_TIME = "allowableTime"

        fun newInstance(result: Result, allowableTime: Long): CheckAnswerFragment =
                CheckAnswerFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARGS_RESULT, moshi.adapter(Result::class.java).toJson(result))
                        putLong(ARGS_ALLOWABLE_TIME, allowableTime)
                    }
                }
    }

    private lateinit var binding: FragmentCheckAnswerBinding

    private lateinit var realm: Realm

    private lateinit var sharedPreferences: SharedPreferences

    private val mainActivity: MainActivity by lazy { activity as MainActivity }

    private val shaperImg: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.glyph_hex_normal)
    }

    private var result: Result? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        realm = Realm.getDefaultInstance()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentCheckAnswerBinding.inflate(inflater, container, false)

        result = arguments?.getString(ARGS_RESULT)?.let {
            moshi.adapter(Result::class.java).fromJson(it)
        }
        binding.result = result
        binding.allowableTime = arguments?.getLong(ARGS_ALLOWABLE_TIME)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recordScore()

        setLeftButton("RETRY") { mainActivity.onRetry() }
        setRightButton("NEXT") { mainActivity.onNext() }

        injectResults()
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }

    private fun recordScore() {
        mainActivity.level?.getDifficulty()?.also { difficulty ->
            when (difficulty) {
                1 -> {
                    val shaper = realm.where(Shaper::class.java)
                            .equalTo("id", mainActivity.sequenceId)
                            .findFirst()

                    shaper?.also { s ->
                        realm.executeTransaction {
                            s.examCount++

                            if (result?.details?.first()?.correct == true)
                                s.correctCount++
                        }
                    }
                }

                else -> {
                    val sequence = realm.where(Sequence::class.java)
                            .equalTo("id", mainActivity.sequenceId)
                            .findFirst()

                    sequence?.also { seq ->
                        realm.executeTransaction {
                            seq.examCount++

                            if (result?.details?.count { it.correct } ?: -1 == result?.details?.size)
                                seq.correctCount++
                        }

                        result?.details?.forEach { detail ->
                            val shaper = realm.where(Shaper::class.java)
                                    .equalTo("id", detail.id)
                                    .findFirst()

                            shaper?.let { s ->
                                realm.executeTransaction {
                                    s.examCount++
                                    if (detail.correct) s.correctCount++
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun injectResults() {
        result?.details?.forEach { detail ->
            val shaper = realm.where(Shaper::class.java)
                    .equalTo("id", detail.id)
                    .findFirst()
                    ?.parse()

            shaper?.let {
                detail.name = it.name
                detail.bitmap = shaperImg.getMutableImageWithShaper(it)
            }
        }

        binding.countHack.visibility =
                if (sharedPreferences.getBooleanValue(Key.SHOW_COUNT)) View.VISIBLE
                else View.GONE
    }

    private fun setRightButton(buttonText: String, predicate: (View) -> Unit) {
        binding.buttonRight.apply {
            text = buttonText
            setOnClickListener(predicate)
            visibility = View.VISIBLE
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
            setOnClickListener(predicate)
            visibility = View.VISIBLE
        }
    }

    private fun hideLeftButton() {
        binding.buttonLeft.apply {
            visibility = View.INVISIBLE
            setOnClickListener(null)
        }
    }
}