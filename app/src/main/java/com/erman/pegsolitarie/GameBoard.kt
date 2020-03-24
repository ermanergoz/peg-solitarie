package com.erman.pegsolitarie

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View

class GameBoard(private var context: Context) {

    private val px = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        GAME_GRID_HOLDER_HEIGHT_MARGIN,
        context.resources.displayMetrics
    )
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels - px.toInt()

    private lateinit var cells: Array<IntArray>

    fun constructEnglishBoard(): View {
        cells = arrayOf(
            intArrayOf(-1, -1, 1, 1, 1, -1, -1),
            intArrayOf(-1, -1, 1, 1, 1, -1, -1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 0, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(-1, -1, 1, 1, 1, -1, -1),
            intArrayOf(-1, -1, 1, 1, 1, -1, -1)
        )   //-1 is dead cell, 1 is peg, 0 is empty
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun constructFrenchBoard(): View {
        cells = arrayOf(
            intArrayOf(-1, -1, 1, 1, 1, -1, -1),
            intArrayOf(-1, 1, 1, 1, 1, 1, -1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 1, 0, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(-1, 1, 1, 1, 1, 1, -1),
            intArrayOf(-1, -1, 1, 1, 1, -1, -1)
        )   //-1 is dead cell, 1 is peg, 0 is empty
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun constructGerman(): View {
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
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun constructAsymmetricalBoard(): View {
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
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun constructDiamondBoard(): View {
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
        return GridView(context, screenWidth, screenHeight, cells, null)
    }

    fun getCells(): Array<IntArray> {
        return this.cells
    }
}