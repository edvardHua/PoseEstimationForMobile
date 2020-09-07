package com.edvard.poseestimation

import java.util.ArrayList

data class Movement(val  bodyPart0_Index: Int, val  bodyPart1_Index: Int, val  bodyPart2_Index: Int) {

    val acceptableAngleVariation : Int = 10

    var  startingAngle: Int? = null
    var  endingAngle: Int? = null

    val executionTime: Float? = null

    var angleAvg: Int? = null
    val angleValuesLastFrames = ArrayList<Double>()

    var movementState: Int= 0

}