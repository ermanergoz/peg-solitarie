package com.erman.pegsolitarie.game.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.erman.pegsolitarie.utils.KEY_QUIT_BUTTON
import com.erman.pegsolitarie.utils.KEY_RESTART_BUTTON
import com.erman.pegsolitarie.R

class GameOverDialog(private var elapsedTime: String, private var score: String) : DialogFragment() {
    private lateinit var listener: GameOverDialogListener
    private lateinit var elapsedTimeTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var restartButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var dialogView: View

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog = activity?.let {
            val builder = AlertDialog.Builder(it, R.style.DialogFragmentTheme)
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
            throw ClassCastException(("$context must implement GameOverDialogListener"))
        }
    }

    interface GameOverDialogListener {
        fun gameOverDialogListener(chosenAction: String)
    }
}