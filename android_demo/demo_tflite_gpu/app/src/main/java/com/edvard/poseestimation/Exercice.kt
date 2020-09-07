package com.edvard.poseestimation

import android.graphics.PointF
import java.util.ArrayList
import kotlin.math.roundToInt

class Exercice {

    val maxExecutionTime: Float? = null
    val minExecutionTime: Float? = null

    var numberOfRepetitionToDo: Int? = null
    var numberOfRepetition: Int = 0
    var numberOfRepititionReached: Boolean = false

    val simultaneousMovement: Boolean? = null
    val movementList = ArrayList<Movement>()

    fun exerciceVerification(drawView: DrawView)
    {
        movementList.forEach()
        {
            calculateAngle(it!!, drawView!!)
            if(isAngleMatching(it!!))
            {
                when(it!!.movementState)
                {
                    0 -> {it.movementState = 1}
                    1 -> {it.movementState = 2}
                    2 ->
                    {
                        it.movementState = 1
                        numberOfRepetition++
                    }
                }
            }
        }

        if(numberOfRepetitionToDo != null)
        {
            if(numberOfRepetitionToDo == numberOfRepetition)
            {
                numberOfRepititionReached = true
            }
        }
    }

    private fun calculateAngle(movement: Movement, drawView: DrawView)
    {
        var p0: PointF = drawView.mDrawPoint[movement.bodyPart0_Index]
        var p1: PointF = drawView.mDrawPoint[movement.bodyPart1_Index]
        var p2: PointF = drawView.mDrawPoint[movement.bodyPart2_Index]

        var deltaX1 = (p0.x -p1.x)
        var deltaY1 = (p0.y -p1.y)

        var deltaX2 = (p2.x-p1.x)
        var deltaY2 = (p2.y-p1.y)

        var m1 = deltaY1/deltaX1
        var m2 = deltaY2/deltaX2

        var angle = ((kotlin.math.atan(Math.abs((m2 - m1) / (1 + (m2 * m1)))) * 180) / Math.PI)

        if(deltaX1*deltaX2 >= 0 || deltaY1*deltaY2 >= 0)
            //Do nothing
        else
            angle = 180 - angle

        if(! angle.isNaN())
        {
            if(movement.angleValuesLastFrames.size == drawView.frameCounterMax)
            {
                movement.angleValuesLastFrames.removeAt(0)
            }
            movement.angleValuesLastFrames.add(angle)
        }

        if(movement.angleValuesLastFrames.size != 0)
            movement.angleAvg = movement.angleValuesLastFrames.average().roundToInt()
    }

    fun isAngleMatching(movement: Movement): Boolean
    {
        if(movement.angleAvg != null)
        {
            when(movement.movementState)
            {
                0,2 -> {return movement.angleAvg!! > movement.startingAngle!! - movement.acceptableAngleVariation!! && movement.angleAvg!! < movement.startingAngle!! + movement.acceptableAngleVariation!!}
                1 -> {return movement.angleAvg!! > movement.endingAngle!! - movement.acceptableAngleVariation!! && movement.angleAvg!! < movement.endingAngle!! + movement.acceptableAngleVariation!!}
                else -> {return false}
            }
        }
        else
        {
            return false
        }
    }

}