package com.erman.pegsolitarie.game.model

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import com.erman.pegsolitarie.game.view.GridView
import com.erman.pegsolitarie.utils.*

class GameBoard(private var context: Context) {

    private val px = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        GAME_GRID_HOLDER_HEIGHT_MARGIN,
        context.resources.displayMetrics
    )
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels - px.toInt()

    private lateinit var cells: Array<IntArray>

    fun constructGameBoard(selectedBoard: String): View {
        when (selectedBoard) {
            ENGLISH_BOARD ->{
                cells = arrayOf(
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(1, 1, 1, 0, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1)
                )   //-1 is dead cell, 1 is peg, 0 is empty
            }
            FRENCH_BOARD -> {
                cells = arrayOf(
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, 1, 1, 1, 1, 1, -1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(1, 1, 0, 1, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(-1, 1, 1, 1, 1, 1, -1),
                    intArrayOf(-1, -1, 1, 1, 1, -1, -1)
                )   //-1 is dead cell, 1 is peg, 0 is empty
            }
            GERMAN_BOARD ->{
                cells = arrayOf(
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1)
                )   //-1 is dead cell, 1 is peg, 0 is empty
            }
            ASYMMETRICAL_BOARD -> {
                cells = arrayOf(
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 0, 1, 1, 1),
                    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1)
                )   //-1 is dead cell, 1 is peg, 0 is empty
            }
            DIAMOND_BOARD -> {
                cells = arrayOf(
                    intArrayOf(-1, -1, -1, -1, 1, -1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, 1, 1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, 1, 1, 1, 1, 1, 1, 1, -1),
                    intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1),
                    intArrayOf(-1, 1, 1, 1, 1, 1, 1, 1, -1),
                    intArrayOf(-1, -1, 1, 1, 1, 1, 1, -1, -1),
                    intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1, -1),
                    intArrayOf(-1, -1, -1, -1, 1, -1, -1, -1, -1)
                )   //-1 is dead cell, 1 is peg, 0 is empty
            }
        }
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun getCells(): Array<IntArray> {
        return this.cells
    }

    fun updateCells(cells: Array<IntArray>): View {
        this.cells = cells
        return GridView(context, screenWidth, screenHeight, cells, null)
    }
}