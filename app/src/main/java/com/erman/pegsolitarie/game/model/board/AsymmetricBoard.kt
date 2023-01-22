package com.erman.pegsolitarie.game.model.board

class AsymmetricBoard(
    override var cells: Array<IntArray> = arrayOf(
        intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
        intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 0, 1, 1, 1),
        intArrayOf(1, 1, 1, 1, 1, 1, 1, 1),
        intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
        intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1),
        intArrayOf(-1, -1, -1, 1, 1, 1, -1, -1)
    )   //-1 is dead cell, 1 is peg, 0 is empty
) : Board()