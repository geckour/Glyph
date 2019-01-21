package jp.org.example.geckour.glyph.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.DialogPickNumberBinding

class LevelPickDialogFragment : DialogFragment() {

    enum class LevelType {
        MIN,
        MAX
    }

    companion object {
        val tag: String = LevelPickDialogFragment::class.java.simpleName

        private const val ARGS_TYPE = "type"
        private const val ARGS_MIN = "min"
        private const val ARGS_MAX = "max"

        fun newInstance(type: LevelType, min: Int?, max: Int?): LevelPickDialogFragment =
                LevelPickDialogFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable(ARGS_TYPE, type)
                        if (min != null) putInt(ARGS_MIN, min)
                        if (max != null) putInt(ARGS_MAX, max)
                    }
                }
    }

    private lateinit var binding: DialogPickNumberBinding
    private lateinit var type: LevelType
    private var min: Int = 0
    private var max: Int = 8

    var onConfirm: (Int) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.apply {
            type = getSerializable(ARGS_TYPE) as LevelType
            if (containsKey(ARGS_MIN)) min = getInt(ARGS_MIN, 0)
            if (containsKey(ARGS_MAX)) max = getInt(ARGS_MAX, 8)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = activity ?: throw IllegalStateException()

        binding = DialogPickNumberBinding.inflate(fragmentActivity.layoutInflater, null, false)

        binding.apply {
            title = when (type) {
                LevelType.MIN -> "Minimum Portal level"
                LevelType.MAX -> "Maximum Portal level"
            }
            message = when (type) {
                LevelType.MIN -> "Define the lower limit of the portal level to choose sequence"
                LevelType.MAX -> "Define the upper limit of the portal level to choose sequence"
            }
            numberPicker.apply {
                minValue = 0
                maxValue = 8
                value = when (type) {
                    LevelType.MIN -> min
                    LevelType.MAX -> max
                }
                wrapSelectorWheel = false
            }
        }

        return AlertDialog.Builder(fragmentActivity, R.style.AppThemeDark_Dialog)
                .setView(binding.root)
                .setPositiveButton("OK") { _, _ -> onConfirm(binding.numberPicker.value) }
                .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }.create()

    }
}