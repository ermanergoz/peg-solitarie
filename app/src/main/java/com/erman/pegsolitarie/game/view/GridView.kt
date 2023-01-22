package com.erman.pegsolitarie.game.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat.getColor
import com.erman.pegsolitarie.R
import com.erman.pegsolitarie.utils.PEG_MARGIN
import com.erman.pegsolitarie.utils.PEG_SLOT_WIDTH

@SuppressLint("ViewConstructor")
class GridView(
    context: Context?,
    private var screenWidth: Int,
    private var screenHeight: Int,
    private var cells: Array<IntArray>,
    attrs: AttributeSet?
) :
    View(context, attrs) {
    private var cellWidth = 0
    private var cellHeight = 0
    private val pegPaint: Paint = Paint()
    private val pegSlotPaint: Paint = Paint()
    private val markedPegPaint: Paint = Paint()
    private var isFirstClick = true
    private var columnFirst = 0
    private var rowFirst = 0
    private var columnSecond = 0
    private var rowSecond = 0
    private lateinit var listener: GridViewListener

    private fun calculateDimensions() {
        cellWidth = screenWidth / cells.size
        cellHeight = (screenHeight) / cells[0].size
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        paintCells(canvas)
    }

    private fun getCenterCoordinates(column: Int, row: Int): Pair<Float, Float> {
        val centerX = (((column * cellWidth) + (((column + 1) * cellWidth))) / 2).toFloat()
        val centerY = (((row * cellHeight) + (((row + 1) * cellHeight))) / 2).toFloat()

        return Pair(centerX, centerY)
    }

    private fun getRadius(): Float {
        return if (cellWidth < cellHeight)
            (cellWidth / 2).toFloat()
        else (cellHeight / 2).toFloat()
    }

    private fun drawBoardItems(centerPoint:Pair<Float, Float>, radius:Float, canvas: Canvas, paint: Paint) {
        canvas.drawCircle(centerPoint.first, centerPoint.second, radius - PEG_MARGIN, paint)
    }

    private fun paintCells(canvas: Canvas) {
        val radius = getRadius()
        for (i in cells.indices) {
            for (j in cells[i].indices) {
                val centerPoint = getCenterCoordinates(i, j)
                if (cells[i][j] == 1)
                    drawBoardItems(centerPoint, radius, canvas, pegPaint)
                else if (cells[i][j] == 0)
                    drawBoardItems(centerPoint, radius,canvas, pegSlotPaint)
                if (cells[i][j] == 2) {
                    drawBoardItems(centerPoint, radius,canvas, markedPegPaint)
                }
            }
        }
    }

    private fun canMarkSelectedPeg(column: Int, row: Int): Boolean {
        if ((row - 1 >= 0 && row - 2 >= 0 && cells[column][row] == 1 && cells[column][row - 1] == 1 && cells[column][row - 2] == 0/*north*/) ||
            (row + 1 < cells[column].size && row + 2 < cells.size && cells[column][row] == 1 && cells[column][row + 1] == 1 && cells[column][row + 2] == 0 /*south*/) ||
            (column + 1 < cells.size && column + 2 < cells.size && cells[column][row] == 1 && cells[column + 1][row] == 1 && cells[column + 2][row] == 0 /*east*/) ||
            (column - 1 >= 0 && column - 2 >= 0 && cells[column][row] == 1 && cells[column - 1][row] == 1 && cells[column - 2][row] == 0 /*west*/)
        )
            return true
        return false
    }

    private fun markPeg(column: Int, row: Int) {
        cells[column][row] = 2
        invalidate()
    }

    private fun unMarkPeg(column: Int, row: Int) {
        cells[column][row] = 1
        invalidate()
    }

    private fun cancelMove() {
        unMarkPeg(columnFirst, rowFirst)
        isFirstClick = true
        invalidate()
    }

    private fun arePegsValid(): Boolean {
        return cells[columnFirst][rowFirst] != -1 && cells[columnSecond][rowSecond] != -1
    }

    private fun arePegsSame(): Boolean {
        return columnFirst == columnSecond && rowFirst == rowSecond
    }

    private fun isSecondCellValid(): Boolean {
        return cells[columnSecond][rowSecond] != -1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            if (isFirstClick) {
                columnFirst = (event.x / cellWidth).toInt()
                rowFirst = (event.y / cellHeight).toInt()
                if (canMarkSelectedPeg(columnFirst, rowFirst)) {
                    isFirstClick = false
                    markPeg(columnFirst, rowFirst)
                }
            } else {
                columnSecond = (event.x / cellWidth).toInt()
                rowSecond = (event.y / cellHeight).toInt()

                if (arePegsSame()) {
                    cancelMove()
                    return false
                }
                if (rowFirst < cells[0].size && columnFirst < cells.size) {
                    if (arePegsValid()) {
                        unMarkPeg(columnFirst, rowFirst)
                        performClick()
                    } else if (!isSecondCellValid()) {
                        cancelMove()
                        return false
                    }
                    isFirstClick = true
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        listener.onGridViewTouch(cells, rowFirst, columnFirst, rowSecond, columnSecond)
        return true
    }

    init {
        calculateDimensions()
        context?.let {
            markedPegPaint.color = getColor(it, R.color.markedPegColor)
            markedPegPaint.style = Paint.Style.FILL_AND_STROKE
            pegPaint.color = getColor(it, R.color.pegColor)
            pegPaint.style = Paint.Style.FILL_AND_STROKE
            pegSlotPaint.style = Paint.Style.FILL_AND_STROKE
            pegSlotPaint.strokeWidth = PEG_SLOT_WIDTH
            pegSlotPaint.color = getColor(it, R.color.pegSlotColor)
        }
        try {
            listener = context as GridViewListener
        } catch (err: ClassCastException) {
            Log.e("error", "must implement GridViewListener")
        }
    }
}

interface GridViewListener {
    fun onGridViewTouch(
        cells: Array<IntArray>,
        rowFirst: Int,
        columnFirst: Int,
        rowSecond: Int,
        columnSecond: Int
    )
}