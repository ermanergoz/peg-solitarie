package com.erman.pegsolitarie.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.erman.pegsolitarie.*
import com.erman.pegsolitarie.game.view.GameActivity
import com.erman.pegsolitarie.game.data.Scores
import com.erman.pegsolitarie.utils.*
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_home.*
import java.text.SimpleDateFormat

class HomeActivity : AppCompatActivity() {

    private lateinit var realm: Realm
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
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
        getDataFromDatabase(FRENCH_BOARD)?.let { frenchBoardData ->
            if (frenchBoardData.isNotEmpty()) {
                "${frenchBoardData[0].remainingPegs} / 36".also { frenchScoreTextView.text = it }
                frenchTimeTextView.text = dateFormat.format(frenchBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(GERMAN_BOARD)?.let { germanBoardData ->
            if (germanBoardData.isNotEmpty()) {
                (germanBoardData[0].remainingPegs.toString() + " / 44").also {
                    germanScoreTextView.text = it
                }
                germanTimeTextView.text = dateFormat.format(germanBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(ASYMMETRICAL_BOARD)?.let { asymmetricalBoardData ->
            if (asymmetricalBoardData.isNotEmpty()) {
                (asymmetricalBoardData[0].remainingPegs.toString() + " / 38").also {
                    asymmetricScoreTextView.text = it
                }
                asymmetricTimeTextView.text =
                    dateFormat.format(asymmetricalBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(ENGLISH_BOARD)?.let { englishBoardData ->
            if (englishBoardData.isNotEmpty()) {
                (englishBoardData[0].remainingPegs.toString() + " / 32").also {
                    englishScoreTextView.text = it
                }
                englishTimeTextView.text = dateFormat.format(englishBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(DIAMOND_BOARD)?.let { diamondBoardData ->
            if (diamondBoardData.isNotEmpty()) {
                (diamondBoardData[0].remainingPegs.toString() + " / 40").also {
                    diamondScoreTextView.text = it
                }
                diamondTimeTextView.text = dateFormat.format(diamondBoardData[0].elapsedTime)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBestScores()
    }
}