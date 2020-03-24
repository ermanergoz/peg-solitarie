package com.erman.pegsolitarie

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment


class GamePausedDialog : DialogFragment() {
    private lateinit var listener: GamePausedDialogListener
    private lateinit var resumeButton: Button
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog = activity?.let {
            val builder = AlertDialog.Builder(context)
            val inflater = requireActivity().layoutInflater
            dialogView = inflater.inflate(R.layout.dialog_paused, null)

            this.resumeButton = dialogView.findViewById(R.id.resumeButton)

            resumeButton.setOnClickListener {
                listener.resumeGame()
                dialog?.dismiss()
            }

            builder.setMessage(getString(R.string.game_paused))
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
            throw ClassCastException((context.toString() + " must implement GamePausedDialogListener"))
        }
    }

    interface GamePausedDialogListener {
        fun resumeGame()
    }
}