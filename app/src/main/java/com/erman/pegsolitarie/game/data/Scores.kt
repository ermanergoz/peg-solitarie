package com.erman.pegsolitarie.game.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Scores (
    @PrimaryKey var id: Int = 0,
    var gameBoard: String = "",
    var remainingPegs: Int = 0,
    var elapsedTime: Long = 0
) : RealmObject()