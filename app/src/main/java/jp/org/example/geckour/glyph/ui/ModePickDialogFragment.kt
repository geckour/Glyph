package jp.org.example.geckour.glyph.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.DialogPickListBinding
import jp.org.example.geckour.glyph.util.HintType

class ModePickDialogFragment : DialogFragment() {

    companion object {
        val tag: String = ModePickDialogFragment::class.java.simpleName

        private const val ARGS_CURRENT = "current"

        fun newInstance(type: Int): ModePickDialogFragment =
                ModePickDialogFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARGS_CURRENT, type)
                    }
                }
    }

    private lateinit var binding: DialogPickListBinding
    private var current: Int = 0

    var onConfirm: (Int) -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        current = arguments?.getInt(ARGS_CURRENT, 0) ?: 0
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = activity ?: throw IllegalStateException()

        binding = DialogPickListBinding.inflate(fragmentActivity.layoutInflater, null, false)

        binding.apply {
            title = "Game mode"
            message = "Define the mode how to show sequence"
            spinner.apply {
                adapter = ArrayAdapter<String>(activity,
                        android.R.layout.simple_spinner_dropdown_item,
                        HintType.values().map { it.displayName })

                setSelection(current)
            }
        }

        return AlertDialog.Builder(fragmentActivity, R.style.AppThemeDark_Dialog)
                .setView(binding.root)
                .setPositiveButton("OK") { _, _ -> onConfirm(binding.spinner.selectedItemPosition) }
                .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }.create()

    }
}