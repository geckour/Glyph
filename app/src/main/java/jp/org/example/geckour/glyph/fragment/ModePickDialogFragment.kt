package jp.org.example.geckour.glyph.fragment

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.PrefActivity
import jp.org.example.geckour.glyph.databinding.DialogPickListBinding

class ModePickDialogFragment : DialogFragment() {

    companion object {
        val tag: String = ModePickDialogFragment::class.java.simpleName

        private val ARGS_CURRENT = "current"

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

        current = arguments.getInt(ARGS_CURRENT, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogPickListBinding.inflate(activity.layoutInflater)

        binding.apply {
            title = "Game mode"
            message = "Define the mode how to show sequence"
            spinner.apply {
                adapter = ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, PrefActivity.HintType.values().map { it.displayName })
                setSelection(current)
            }
        }

        return AlertDialog.Builder(activity, R.style.AppThemeDark_Dialog)
                .setView(binding.root)
                .setPositiveButton("OK") { _, _ -> onConfirm(binding.spinner.selectedItemPosition) }
                .setNegativeButton("CANCEL") { dialog, _ -> dialog.cancel() }.create()

    }
}