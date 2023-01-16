package com.erman.pegsolitarie.game.view

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.erman.pegsolitarie.*
import com.erman.pegsolitarie.databinding.ActivityGameBinding
import com.erman.pegsolitarie.game.data.Scores
import com.erman.pegsolitarie.game.dialog.GameMenuDialog
import com.erman.pegsolitarie.game.dialog.GameOverDialog
import com.erman.pegsolitarie.game.dialog.GamePausedDialog
import com.erman.pegsolitarie.game.model.GameBoard
import com.erman.pegsolitarie.game.model.getPegCount
import com.erman.pegsolitarie.game.model.isGameOver
import com.erman.pegsolitarie.game.model.movePegToDirection
import com.erman.pegsolitarie.utils.KEY_GAME_BOARD
import com.erman.pegsolitarie.utils.KEY_QUIT_BUTTON
import com.erman.pegsolitarie.utils.KEY_RESTART_BUTTON
import com.erman.pegsolitarie.utils.KEY_SETTINGS_BUTTON
import com.google.android.material.snackbar.Snackbar
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class GameActivity : AppCompatActivity(), GridViewListener, GameOverDialog.GameOverDialogListener,
    GameMenuDialog.GameMenuDialogListener, GamePausedDialog.GamePausedDialogListener {
    private lateinit var viewBinding: ActivityGameBinding
    private lateinit var boardSelection: String
    private val fragmentManager: FragmentManager = supportFragmentManager
    private var scoreText = ""
    private lateinit var gameBoard: GameBoard
    private var timeWhenChronoStopped: Long = 0
    private lateinit var realm: Realm
    private var prevMoves: MutableList<Array<IntArray>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        @SuppressLint("SourceLockedOrientationActivity")
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        realm = Realm.getDefaultInstance()

        intent.getStringExtra(KEY_GAME_BOARD)?.let { boardSelection = it }
        createGameBoard()

        viewBinding.pauseButton.setOnClickListener {
            pauseGame()
        }

        viewBinding.gameMenuButton.setOnClickListener {
            val dialog = GameMenuDialog()
            dialog.show(fragmentManager, "")
        }

        viewBinding.revertButton.setOnClickListener {
            if (prevMoves.isNotEmpty()) {
                viewBinding.gameGridHolder.removeAllViews()
                viewBinding.gameGridHolder.addView(gameBoard.updateCells(prevMoves[prevMoves.lastIndex]))
                prevMoves.removeAt(prevMoves.lastIndex)
                updateScoreTextView()
            }
        }
    }

    private fun createGameBoard() {
        gameBoard = GameBoard(this)
        viewBinding.gameGridHolder.addView(gameBoard.constructGameBoard(boardSelection))
        viewBinding.elapsedTimeChronometer.start()
    }

    private fun onGameOver(scoreText: String) {
        val dialog = GameOverDialog(viewBinding.elapsedTimeChronometer.text.toString(), scoreText)
        dialog.show(fragmentManager, "")
        viewBinding.elapsedTimeChronometer.stop()
        viewBinding.elapsedTimeChronometer.base = SystemClock.elapsedRealtime() //to reset it
    }

    private fun resetChronometer() {
        viewBinding.elapsedTimeChronometer.base = SystemClock.elapsedRealtime()
    }

    private fun resetGameBoard() {
        viewBinding.gameGridHolder.removeAllViews()
        createGameBoard()
        resetScoreTextView()
        resetChronometer()
    }

    private fun updateScoreTextView() {
        val pegCount = getPegCount(gameBoard.getCells())
        this.scoreText = pegCount.first.toString() + " / " + pegCount.second
        viewBinding.scoreTextView.text = scoreText
    }

    private fun resetScoreTextView() {
        viewBinding.scoreTextView.text = ""
    }

    private fun pauseGame() {
        timeWhenChronoStopped = SystemClock.elapsedRealtime() - viewBinding.elapsedTimeChronometer.base
        viewBinding.elapsedTimeChronometer.stop()

        val dialog = GamePausedDialog()
        dialog.show(fragmentManager, "")
    }

    private fun Array<IntArray>.copy() = map { it.clone() }.toTypedArray()  //deep copy

    private fun updateDatabase() {
        realm.beginTransaction()
        val score: Scores = realm.createObject<Scores>((realm.where<Scores>().findAll().size) + 1)
        score.gameBoard = boardSelection
        score.elapsedTime = SystemClock.elapsedRealtime() - viewBinding.elapsedTimeChronometer.base
        score.remainingPegs = getPegCount(gameBoard.getCells()).first
        realm.commitTransaction()
    }

    override fun onGridViewTouch(
        cells: Array<IntArray>,
        rowFirst: Int,
        columnFirst: Int,
        rowSecond: Int,
        columnSecond: Int
    ) {
        prevMoves.add(cells.copy())
        if (!movePegToDirection(cells, rowFirst, columnFirst, rowSecond, columnSecond)) {
            Snackbar.make(viewBinding.parentLayout, getString(R.string.invalid_move), Snackbar.LENGTH_SHORT).show()
        }
        updateScoreTextView()

        if (isGameOver(cells)) {
            updateDatabase()
            onGameOver(scoreText)
        }
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
        viewBinding.elapsedTimeChronometer.base = SystemClock.elapsedRealtime() - timeWhenChronoStopped
        viewBinding.elapsedTimeChronometer.start()
    }
}