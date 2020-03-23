package com.erman.pegsolitarie

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.toast_notification_layout.*
import kotlinx.android.synthetic.main.toast_notification_layout.view.*

class GameActivity : AppCompatActivity(), GridViewListener, GameOverDialog.GameOverDialogListener,
    GameMenuDialog.GameMenuDialogListener, GamePausedDialog.GamePausedDialog {

    private lateinit var boardSelection: String
    private val fragmentManager: FragmentManager = supportFragmentManager
    private var scoreText = ""
    private lateinit var gameGrid: View
    private lateinit var gameBoard: GameBoard
    private var timeWhenChronoStopped: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        boardSelection = intent.getStringExtra(KEY_GAME_BOARD)!!
        createGameBoard()

        pauseButton.setOnClickListener {
            pauseGame()
        }

        gameMenuButton.setOnClickListener {
            val dialog = GameMenuDialog()
            dialog.show(fragmentManager, "")
        }
    }

    private fun createGameBoard() {
        if (boardSelection == ENGLISH_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructEnglishBoard())
            elapsedTimeChronometer.start()
        } else if (boardSelection == FRENCH_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructFrenchBoard())
            elapsedTimeChronometer.start()
        } else if (boardSelection == GERMAN_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructGerman())
            elapsedTimeChronometer.start()
        } else if (boardSelection == ASYMMETRICAL_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructAsymmetricalBoard())
            elapsedTimeChronometer.start()
        } else if (boardSelection == DIAMOND_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructDiamondBoard())
            elapsedTimeChronometer.start()
        } else if (boardSelection == TRIANGULAR_BOARD) {
            gameBoard = GameBoard(this)
            gameGridHolder.addView(gameBoard.constructTriangularBoard())
            elapsedTimeChronometer.start()
        }
    }

    private fun onGameOver(scoreText: String) {
        val dialog = GameOverDialog(elapsedTimeChronometer.text.toString(), scoreText)
        dialog.show(fragmentManager, "")
        elapsedTimeChronometer.stop()
        elapsedTimeChronometer.base = SystemClock.elapsedRealtime() //to reset it
    }

    private fun resetGameBoard() {
        gameGridHolder.removeAllViews()
        createGameBoard()
        resetScoreTextView()
    }

    private fun updateScoreTextView() {
        val pegCount = getPegCount(gameBoard.getCells())
        this.scoreText = pegCount.first.toString() + " / " + pegCount.second
        scoreTextView.text = scoreText
    }

    private fun resetScoreTextView() {
        scoreTextView.text = ""
    }

    private fun pauseGame() {
        timeWhenChronoStopped = SystemClock.elapsedRealtime() - elapsedTimeChronometer.base
        elapsedTimeChronometer.stop()

        val dialog = GamePausedDialog()
        dialog.show(fragmentManager, "")
    }

    override fun onGridViewTouch(
        cells: Array<IntArray>,
        rowFirst: Int,
        columnFirst: Int,
        rowSecond: Int,
        columnSecond: Int
    ) {
        if (!movePegToDirection(cells, rowFirst, columnFirst, rowSecond, columnSecond)) {
            //val layout: View =
            with (Toast(applicationContext)) {
                setGravity(Gravity.TOP, 0, 0)
                duration = Toast.LENGTH_SHORT
                view = layoutInflater.inflate(R.layout.toast_notification_layout, toastContainer)
                view.textView.text=getString(R.string.invalid_move)
                show()
            }
        }

        updateScoreTextView()

        if (isGameOver(cells))
            onGameOver(scoreText)
    }

    override fun gameOverDialogListener(chosenAction: String) {
        when (chosenAction) {
            KEY_RESTART_BUTTON ->
                resetGameBoard()
            KEY_QUIT_BUTTON ->
                finish()
        }
    }

    override fun gameMenuDialogListener(chosenAction: String) {
        when (chosenAction) {
            KEY_RESTART_BUTTON ->
                resetGameBoard()
            KEY_QUIT_BUTTON ->
                finish()
            KEY_SETTINGS_BUTTON ->
                Log.e("settings button", "pressed")
        }
    }

    override fun resumeGame() {
        elapsedTimeChronometer.base = SystemClock.elapsedRealtime() - timeWhenChronoStopped
        elapsedTimeChronometer.start()
    }
}