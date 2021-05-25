package com.erman.pegsolitarie.game.model

fun movePegToDirection(cells: Array<IntArray>, rowFirst: Int, columnFirst: Int, rowSecond: Int, columnSecond: Int): Boolean {

    if (cells[columnFirst][rowFirst] == 1 && cells[columnSecond][rowSecond] == 0) {
        if (rowFirst - 1 >= 0 && cells[columnFirst][rowFirst - 1] == 1 && rowFirst - rowSecond == 2  && columnFirst == columnSecond /*move up*/) {
            collectPeg(cells, columnFirst, rowFirst - 1)
            movePegToEmptySlot(cells, rowFirst, columnFirst, rowSecond, columnSecond)
            return true
        } else if (rowFirst + 1 < cells.size && cells[columnFirst][rowFirst + 1] == 1 && rowSecond - rowFirst == 2  && columnFirst == columnSecond /*move down*/) {
            collectPeg(cells, columnFirst, rowFirst + 1)
            movePegToEmptySlot(cells, rowFirst, columnFirst, rowSecond, columnSecond)
            return true
        } else if (columnFirst + 1 < cells.size && cells[columnFirst + 1][rowFirst] == 1 && columnSecond - columnFirst == 2 && rowFirst == rowSecond /*move right*/) {
            collectPeg(cells, columnFirst + 1, rowFirst)
            movePegToEmptySlot(cells, rowFirst, columnFirst, rowSecond, columnSecond)
            return true
        } else if (columnFirst - 1 >= 0 && cells[columnFirst - 1][rowFirst] == 1 && columnFirst - columnSecond == 2 && rowFirst == rowSecond /*move left*/) {
            collectPeg(cells, columnFirst - 1, rowFirst)
            movePegToEmptySlot(cells, rowFirst, columnFirst, rowSecond, columnSecond)
            return true
        }
    }
    return false
}

private fun movePegToEmptySlot(cells: Array<IntArray>, rowFirst: Int, columnFirst: Int, rowSecond: Int, columnSecond: Int) {
    cells[columnSecond][rowSecond] = 1
    cells[columnFirst][rowFirst] = 0
}

private fun collectPeg(cells: Array<IntArray>, column: Int, row: Int) {
    cells[column][row] = 0
}

fun isGameOver(cells: Array<IntArray>): Boolean {
    for (column in cells.indices) {
        for (row in cells[column].indices) {
            if (cells[column][row] != -1 && cells[column][row] == 1)
                if ((row - 1 >= 0 && row - 2 >= 0 && cells[column][row - 1] == 1 && cells[column][row - 2] == 0/*north*/) ||
                    (row + 1 < cells[column].size && row + 2 < cells.size && cells[column][row + 1] == 1 && cells[column][row + 2] == 0 /*south*/) ||
                    (column + 1 < cells.size && column + 2 < cells.size && cells[column + 1][row] == 1 && cells[column + 2][row] == 0 /*east*/) ||
                    (column - 1 >= 0 && column - 2 >= 0 && cells[column - 1][row] == 1 && cells[column - 2][row] == 0 /*west*/)
                )
                    return false
        }
    }
    return true
}

fun getPegCount(cells: Array<IntArray>): Pair<Int, Int> {
    var takenPegs = -1
    var totalPegs = -1

    for (column in cells.indices) {
        for (row in cells[column].indices) {
            if (cells[row][column] == 0)
                takenPegs++
            if (cells[row][column] != -1)
                totalPegs++
        }
    }
    return Pair(totalPegs - takenPegs, totalPegs)
}