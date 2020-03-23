package com.erman.pegsolitarie

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment

class GameOverDialog(private var elapsedTime: String, private var score: String) : DialogFragment() {
    private lateinit var listener: GameOverDialogListener
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var restartButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog = activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            dialogView = inflater.inflate(R.layout.dialog_game_over, null)

            this.elapsedTimeTextView = dialogView.findViewById(R.id.elapsedTimeTextView)
            this.scoreTextView = dialogView.findViewById(R.id.scoreTextView)
            this.restartButton = dialogView.findViewById(R.id.restartButton)
            this.mainMenuButton = dialogView.findViewById(R.id.quitButton)

            elapsedTimeTextView.text = elapsedTime
            scoreTextView.text = score

            restartButton.setOnClickListener {
                listener.gameOverDialogListener(KEY_RESTART_BUTTON)
                dialog?.dismiss()
            }

            mainMenuButton.setOnClickListener {
                listener.gameOverDialogListener(KEY_QUIT_BUTTON)
                dialog?.dismiss()
            }

            builder.setMessage(getString(R.string.game_over))
            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")

        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as GameOverDialogListener
        } catch (err: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement GameOverDialogListener"))
        }
    }

    interface GameOverDialogListener {
        fun gameOverDialogListener(chosenAction: String)
    }
}