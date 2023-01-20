package com.erman.pegsolitarie.game.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.erman.pegsolitarie.R


class GamePausedDialog : DialogFragment() {
    private lateinit var listener: GamePausedDialogListener
    private lateinit var resumeButton: Button
    private lateinit var dialogView: View

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog = activity?.let {
            val builder = AlertDialog.Builder(context, R.style.DialogFragmentTheme)
            val inflater = requireActivity().layoutInflater
            dialogView = inflater.inflate(R.layout.dialog_paused, null)

            this.resumeButton = dialogView.findViewById(R.id.resumeButton)

            resumeButton.setOnClickListener {
                listener.resumeGame()
                dialog?.dismiss()
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as GamePausedDialogListener
        } catch (err: ClassCastException) {
            throw ClassCastException(("$context must implement GamePausedDialogListener"))
        }
    }

    interface GamePausedDialogListener {
        fun resumeGame()
    }
}