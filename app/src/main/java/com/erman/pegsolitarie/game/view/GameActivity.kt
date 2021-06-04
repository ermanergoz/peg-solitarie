package com.erman.pegsolitarie.game.view

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.erman.pegsolitarie.*
import com.erman.pegsolitarie.game.model.GameBoard
import com.erman.pegsolitarie.game.data.Scores
import com.erman.pegsolitarie.game.dialog.GameMenuDialog
import com.erman.pegsolitarie.game.dialog.GameOverDialog
import com.erman.pegsolitarie.game.dialog.GamePausedDialog
import com.erman.pegsolitarie.game.model.getPegCount
import com.erman.pegsolitarie.game.model.isGameOver
import com.erman.pegsolitarie.game.model.movePegToDirection
import com.erman.pegsolitarie.utils.KEY_GAME_BOARD
import com.erman.pegsolitarie.utils.KEY_QUIT_BUTTON
import com.erman.pegsolitarie.utils.KEY_RESTART_BUTTON
import com.erman.pegsolitarie.utils.KEY_SETTINGS_BUTTON
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.toast_notification_layout.*
import kotlinx.android.synthetic.main.toast_notification_layout.view.*

class GameActivity : AppCompatActivity(), GridViewListener, GameOverDialog.GameOverDialogListener,
    GameMenuDialog.GameMenuDialogListener, GamePausedDialog.GamePausedDialogListener {

    private lateinit var boardSelection: String
    private val fragmentManager: FragmentManager = supportFragmentManager
    private var scoreText = ""
    private lateinit var gameBoard: GameBoard
    private var timeWhenChronoStopped: Long = 0
    private lateinit var realm: Realm
    private var prevMoves: MutableList<Array<IntArray>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        realm = Realm.getDefaultInstance()

            intent.getStringExtra(KEY_GAME_BOARD)?.let { boardSelection =it }
        createGameBoard()

        pauseButton.setOnClickListener {
            pauseGame()
        }

        gameMenuButton.setOnClickListener {
            val dialog = GameMenuDialog()
            dialog.show(fragmentManager, "")
        }

        revertButton.setOnClickListener {
            if (prevMoves.isNotEmpty()) {
                gameGridHolder.removeAllViews()
                gameGridHolder.addView(gameBoard.updateCells(prevMoves[prevMoves.lastIndex]))
                prevMoves.removeAt(prevMoves.lastIndex)
                updateScoreTextView()
            }
        }
    }

    private fun createGameBoard() {
        gameBoard = GameBoard(this)
        gameGridHolder.addView(gameBoard.constructGameBoard(boardSelection))
        elapsedTimeChronometer.start()
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

    private fun Array<IntArray>.copy() = map { it.clone() }.toTypedArray()  //deep copy

    private fun updateDatabase() {
        realm.beginTransaction()
        val score: Scores = realm.createObject<Scores>((realm.where<Scores>().findAll().size) + 1)
        score.gameBoard = boardSelection
        score.elapsedTime = SystemClock.elapsedRealtime() - elapsedTimeChronometer.base
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
            with(Toast(applicationContext)) {
                setGravity(Gravity.TOP, 0, 0)
                duration = Toast.LENGTH_SHORT
                view = layoutInflater.inflate(R.layout.toast_notification_layout, toastContainer)
                view.textView.text = getString(R.string.invalid_move)
                show()
            }
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
        elapsedTimeChronometer.base = SystemClock.elapsedRealtime() - timeWhenChronoStopped
        elapsedTimeChronometer.start()
    }
}