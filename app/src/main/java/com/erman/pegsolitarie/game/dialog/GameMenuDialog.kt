package com.erman.pegsolitarie.game.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.erman.pegsolitarie.utils.KEY_QUIT_BUTTON
import com.erman.pegsolitarie.utils.KEY_RESTART_BUTTON
import com.erman.pegsolitarie.utils.KEY_SETTINGS_BUTTON
import com.erman.pegsolitarie.R

class GameMenuDialog : DialogFragment() {
    private lateinit var listener: GameMenuDialogListener
    private lateinit var settingsButton: Button
    private lateinit var menuRestartButton: Button
    private lateinit var menuQuitButton: Button
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog = activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            dialogView = inflater.inflate(R.layout.dialog_game_menu, null)

            this.settingsButton = dialogView.findViewById(R.id.settingsButton)
            this.menuRestartButton = dialogView.findViewById(R.id.menuRestartButton)
            this.menuQuitButton = dialogView.findViewById(R.id.menuQuitButton)

            menuRestartButton.setOnClickListener {
                listener.gameMenuDialogListener(KEY_RESTART_BUTTON)
                dialog?.dismiss()
            }

            settingsButton.setOnClickListener {
                listener.gameMenuDialogListener(KEY_SETTINGS_BUTTON)
                dialog?.dismiss()
            }

            menuQuitButton.setOnClickListener {
                listener.gameMenuDialogListener(KEY_QUIT_BUTTON)
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
            listener = context as GameMenuDialogListener
        } catch (err: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement GameMenuDialogListener"))
        }
    }

    interface GameMenuDialogListener {
        fun gameMenuDialogListener(chosenAction: String)
    }
}