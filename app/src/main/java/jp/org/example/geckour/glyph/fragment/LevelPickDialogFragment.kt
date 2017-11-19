package jp.org.example.geckour.glyph.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.PrefActivity
import jp.org.example.geckour.glyph.databinding.DialogPickNumberBinding

class LevelPickDialogFragment : DialogFragment() {

    companion object {
        val tag: String = LevelPickDialogFragment::class.java.simpleName

        private val ARGS_TYPE = "type"
        private val ARGS_MIN = "min"
        private val ARGS_MAX = "max"

        fun newInstance(type: PrefActivity.LevelType, min: Int?, max: Int?): LevelPickDialogFragment =
                LevelPickDialogFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARGS_TYPE, type)
                        if (min != null) putInt(ARGS_MIN, min)
                        if (max != null) putInt(ARGS_MAX, max)
                    }
                }
    }

    private lateinit var binding: DialogPickNumberBinding
    private lateinit var type: PrefActivity.LevelType
    private var min: Int = 0
    private var max: Int = 8

    var onConfirm: (Int) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        type = arguments.getSerializable(ARGS_TYPE) as PrefActivity.LevelType
        if (arguments.containsKey(ARGS_MIN)) min = arguments.getInt(ARGS_MIN, 0)
        if (arguments.containsKey(ARGS_MAX)) max = arguments.getInt(ARGS_MAX, 8)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPickNumberBinding.inflate(activity.layoutInflater)

        binding.apply {
            title = when (type) {
                PrefActivity.LevelType.MIN -> "Minimum Portal level"
                PrefActivity.LevelType.MAX -> "Maximum Portal level"
            }
            message = when (type) {
                PrefActivity.LevelType.MIN -> "Define the lower limit of the portal level to choose sequence"
                PrefActivity.LevelType.MAX -> "Define the upper limit of the portal level to choose sequence"
            }
            numberPicker.apply {
                minValue = 0
                maxValue = 8
                value = when (type) {
                    PrefActivity.LevelType.MIN -> min
                    PrefActivity.LevelType.MAX -> max
                }
                wrapSelectorWheel = false
            }
        }

        return AlertDialog.Builder(activity, R.style.AppThemeDark_Dialog)
                .setView(binding.root)
                .setPositiveButton("OK") { _, _ -> onConfirm(binding.numberPicker.value) }
                .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }.create()

    }
}