package com.erman.pegsolitarie.game.model

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import com.erman.pegsolitarie.game.model.board.*
import com.erman.pegsolitarie.game.view.GridView
import com.erman.pegsolitarie.utils.*

class GameBoard(private var context: Context) {
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private lateinit var board: Board

    init {
        val px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            GAME_GRID_HOLDER_HEIGHT_MARGIN,
            context.resources.displayMetrics
        )
        screenWidth = Resources.getSystem().displayMetrics.widthPixels
        screenHeight = Resources.getSystem().displayMetrics.heightPixels - px.toInt()
    }

    fun constructGameBoard(selectedBoard: BoardType): View {
        board = when (selectedBoard) {
            BoardType.FRENCH -> FrenchBoard()
            BoardType.GERMAN -> GermanBoard()
            BoardType.ASYMMETRIC -> AsymmetricBoard()
            BoardType.ENGLISH -> EnglishBoard()
            BoardType.DIAMOND -> DiamondBoard()
        }
        return GridView(context, screenWidth, screenHeight, board.cells, null)
    }

    fun getCells(): Array<IntArray> {
        return this.board.cells
    }

    fun updateCells(cells: Array<IntArray>): View {
        this.board.cells = cells
        return GridView(context, screenWidth, screenHeight, cells, null)
    }
}