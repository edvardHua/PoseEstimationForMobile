package com.epmus.mobile.poseestimation

import java.io.Serializable
import java.util.ArrayList

data class Movement(val  bodyPart0_Index: Int, val  bodyPart1_Index: Int, val  bodyPart2_Index: Int) : Serializable {
    //Const
    val acceptableAngleVariation: Int = 10
    var startingAngle: Int? = null
    var endingAngle: Int? = null
    var isAngleClockWise: Boolean? = null

    val executionTime: Float? = null

    //Var
    var angleAvg: Int? = null
    val angleValuesLastFrames = ArrayList<Double>()

    var movementState: Int = 0
}