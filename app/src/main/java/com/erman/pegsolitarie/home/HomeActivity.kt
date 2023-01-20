package com.erman.pegsolitarie.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.erman.pegsolitarie.*
import com.erman.pegsolitarie.databinding.ActivityHomeBinding
import com.erman.pegsolitarie.game.view.GameActivity
import com.erman.pegsolitarie.game.data.Scores
import com.erman.pegsolitarie.game.model.BoardType
import com.erman.pegsolitarie.utils.*
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import java.text.SimpleDateFormat

class HomeActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityHomeBinding
    private lateinit var realm: Realm
    @SuppressLint("SimpleDateFormat")
    private val dateFormat = SimpleDateFormat(SIMPLE_DATE_FORMAT_PATTERN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityHomeBinding.inflate(layoutInflater)
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
        //File(this.filesDir.path).deleteRecursively()
        Realm.init(this)
        val config = RealmConfiguration.Builder().name(REALM_CONFIG_FILE_NAME).build()
        Realm.setDefaultConfiguration(config)
        realm = Realm.getDefaultInstance()

        updateBestScores()

        val intent = Intent(this, GameActivity::class.java)
        viewBinding.englishButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, BoardType.ENGLISH)
            startActivity(intent)
        }

        viewBinding.frenchButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, BoardType.FRENCH)
            startActivity(intent)
        }

        viewBinding.germanButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, BoardType.GERMAN)
            startActivity(intent)
        }

        viewBinding.asymmetricalButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, BoardType.ASYMMETRIC)
            startActivity(intent)
        }

        viewBinding.diamondButton.setOnClickListener {
            intent.putExtra(KEY_GAME_BOARD, BoardType.DIAMOND)
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
                "${frenchBoardData[0].remainingPegs} / 36".also { viewBinding.frenchScoreTextView.text = it }
                viewBinding.frenchTimeTextView.text = dateFormat.format(frenchBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(GERMAN_BOARD)?.let { germanBoardData ->
            if (germanBoardData.isNotEmpty()) {
                (germanBoardData[0].remainingPegs.toString() + " / 44").also {
                    viewBinding.germanScoreTextView.text = it
                }
                viewBinding.germanTimeTextView.text = dateFormat.format(germanBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(ASYMMETRICAL_BOARD)?.let { asymmetricalBoardData ->
            if (asymmetricalBoardData.isNotEmpty()) {
                (asymmetricalBoardData[0].remainingPegs.toString() + " / 38").also {
                    viewBinding.asymmetricScoreTextView.text = it
                }
                viewBinding.asymmetricTimeTextView.text =
                    dateFormat.format(asymmetricalBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(ENGLISH_BOARD)?.let { englishBoardData ->
            if (englishBoardData.isNotEmpty()) {
                (englishBoardData[0].remainingPegs.toString() + " / 32").also {
                    viewBinding.englishScoreTextView.text = it
                }
                viewBinding.englishTimeTextView.text = dateFormat.format(englishBoardData[0].elapsedTime)
            }
        }

        getDataFromDatabase(DIAMOND_BOARD)?.let { diamondBoardData ->
            if (diamondBoardData.isNotEmpty()) {
                (diamondBoardData[0].remainingPegs.toString() + " / 40").also {
                    viewBinding.diamondScoreTextView.text = it
                }
                viewBinding.diamondTimeTextView.text = dateFormat.format(diamondBoardData[0].elapsedTime)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBestScores()
    }
}