package com.erman.pegsolitarie

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private lateinit var realm: Realm
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        //File(this.filesDir.path).deleteRecursively()
        Realm.init(this)
        val config = RealmConfiguration.Builder().name(REALM_CONFIG_FILE_NAME).build()
        Realm.setDefaultConfiguration(config)
        realm = Realm.getDefaultInstance()

        updateBestScores()

        val intent = Intent(this, GameActivity::class.java)
        englishButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, ENGLISH_BOARD)
            startActivity(intent)
        }

        frenchButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, FRENCH_BOARD)
            startActivity(intent)
        }

        germanButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, GERMAN_BOARD)
            startActivity(intent)
        }

        asymmetricalButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, ASYMMETRICAL_BOARD)
            startActivity(intent)
        }

        diamondButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, DIAMOND_BOARD)
            startActivity(intent)
        }
    }

    private fun getDataFromDatabase(chosenBoard: String): List<Scores>? {
        return realm.where<Scores>().equalTo("gameBoard", chosenBoard)
            .findAll()?.sortedWith(compareBy({ it.remainingPegs }, { it.elapsedTime }))?.toList()
    }

    private fun updateBestScores() {
        val frenchBoardData = getDataFromDatabase(FRENCH_BOARD)
        if (frenchBoardData!!.isNotEmpty()) {
            frenchScoreTextView.text = frenchBoardData[0].remainingPegs.toString() + " / 36"
            frenchTimeTextView.text = dateFormat.format(frenchBoardData.get(0).elapsedTime)
        }

        val germanBoardData = getDataFromDatabase(GERMAN_BOARD)
        if (germanBoardData!!.isNotEmpty()) {
            germanScoreTextView.text = germanBoardData[0].remainingPegs.toString() + " / 44"
            germanTimeTextView.text = dateFormat.format(germanBoardData[0].elapsedTime)
        }

        val asymmetricalBoardData = getDataFromDatabase(ASYMMETRICAL_BOARD)
        if (asymmetricalBoardData!!.isNotEmpty()) {
            asymmetricScoreTextView.text = asymmetricalBoardData[0].remainingPegs.toString()  + " / 38"
            asymmetricTimeTextView.text = dateFormat.format(asymmetricalBoardData[0].elapsedTime)
        }

        val englishBoardData = getDataFromDatabase(ENGLISH_BOARD)
        if (englishBoardData!!.isNotEmpty()) {
            englishScoreTextView.text = englishBoardData[0].remainingPegs.toString() + " / 32"
            englishTimeTextView.text = dateFormat.format(englishBoardData[0].elapsedTime)
        }

        val diamondBoardData = getDataFromDatabase(DIAMOND_BOARD)
        if (diamondBoardData!!.isNotEmpty()) {
            diamondScoreTextView.text = diamondBoardData[0].remainingPegs.toString() + " / 40"
            diamondTimeTextView.text = dateFormat.format(diamondBoardData[0].elapsedTime)
        }
    }

    override fun onResume() {
        super.onResume()
        updateBestScores()
    }
}