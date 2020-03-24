package com.erman.pegsolitarie

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

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
}