package com.epmus.mobile.poseestimation

import android.graphics.PointF
import java.io.Serializable
import java.util.ArrayList
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Exercice: Serializable {
    // add to fun .copy() if there is a modif
    var maxExecutionTime: Float? = null
    var minExecutionTime: Float? = null
    var startTimer: Long? = null
    var lastTimer: Float? = null

    var numberOfRepetitionToDo: Int? = null
    var numberOfRepetition: Int = 0
    var numberOfRepetitionReached: Boolean = false

    var simultaneousMovement: Boolean? = null
    var movementList = ArrayList<Movement>()

    fun exerciceVerification(drawView: DrawView)
    {
        movementList.forEach()
        {

            calculateAngleV2(it!!, drawView!!)

            if(simultaneousMovement == false || simultaneousMovement == null)
            {
                if(isAngleMatching(it!!))
                {
                    when(it!!.movementState)
                    {
                        0 -> {
                            it.movementState = 1
                            startTimer = System.currentTimeMillis()
                        }
                        1 -> {it.movementState = 2}
                        2 ->
                        {
                            it.movementState = 1
                            numberOfRepetition++
                            lastTimer = calculateTime()
                        }
                    }
                }
            }

            else
            {
                if(isAngleMatching(it!!))
                {
                    when(it!!.movementState)
                    {
                        0 -> {
                            it.movementState = 1
                            startTimer = System.currentTimeMillis()
                        }
                        1 -> {it.movementState = 2}
                        2 -> {it.movementState = 3}
                    }
                }
            }
        }

        if(isRepetitionSimultaneousExerciceDone(movementList) == true)
        {
            movementList.forEach()
            {
                it.movementState = 1
            }
            numberOfRepetition++
        }

        if(numberOfRepetitionToDo != null)
        {
            if(numberOfRepetitionToDo == numberOfRepetition)
            {
                numberOfRepetitionReached = true
            }
        }
    }

    private fun isRepetitionSimultaneousExerciceDone(movementList: ArrayList<Movement>): Boolean
    {
        var repetitionDone = true
        movementList.forEach()
        {
            if(it.movementState != 3)
            {
                repetitionDone = false
            }
        }
        return repetitionDone
    }

    /*
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
    */

    fun calculateAngleV2(movement: Movement, drawView: DrawView)
    {
        var pointX0: Float = drawView.mDrawPoint[movement.bodyPart0_Index].x
        var pointY0: Float = drawView.mDrawPoint[movement.bodyPart0_Index].y
        var pointX1: Float = drawView.mDrawPoint[movement.bodyPart1_Index].x
        var pointY1: Float = drawView.mDrawPoint[movement.bodyPart1_Index].y
        var pointX2: Float = drawView.mDrawPoint[movement.bodyPart2_Index].x
        var pointY2: Float = drawView.mDrawPoint[movement.bodyPart2_Index].y

        var X1ToX0: Float = pointX0 - pointX1
        var Y1ToY0: Float = pointY0 - pointY1
        var X1ToX2: Float = pointX2 - pointX1
        var Y1ToY2: Float = pointY2 - pointY1

        var X1X0mod: Float = sqrt((X1ToX0*X1ToX0) + (Y1ToY0*Y1ToY0))
        var X1X2mod: Float = sqrt((X1ToX2*X1ToX2) + (Y1ToY2*Y1ToY2))

        var vectorProduct: Float = X1ToX0 * X1ToX2 + Y1ToY0 * Y1ToY2

        var angleRad: Float = kotlin.math.acos(vectorProduct/(X1X0mod*X1X2mod))
        var angleDeg : Double = ((angleRad*180)/Math.PI).toDouble()

        //Adding anti/clockwise effect
        var a = Y1ToY0/X1ToX0
        var b = pointY0 - (a * pointX0)
        var tmpPointY2 = (a * pointX2) + b
        if (movement.isAngleClockWise!!)
        {
            if (tmpPointY2 < pointY2)
            {
                angleDeg = 360 - angleDeg
            }
        }
        else
        {
            if (tmpPointY2 > pointY2)
            {
                angleDeg = 360 - angleDeg
            }
        }


        if(! angleDeg.isNaN())
        {
            if(movement.angleValuesLastFrames.size == drawView.frameCounterMax)
            {
                movement.angleValuesLastFrames.removeAt(0)
            }
            movement.angleValuesLastFrames.add(angleDeg)
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
                0,2 -> {return movement.angleAvg!! > movement.startingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.startingAngle!! + movement.acceptableAngleVariation}
                1 -> {return movement.angleAvg!! > movement.endingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.endingAngle!! + movement.acceptableAngleVariation}
                else -> {return false}
            }
        }
        else
        {
            return false
        }
    }

    private fun calculateTime (): Float?
    {
        var timeInSecond: Float?= null
        if (startTimer != null)
        {
            timeInSecond = ((System.currentTimeMillis() - startTimer!!).toFloat())/1000
        }
        startTimer = System.currentTimeMillis()
        return timeInSecond
    }

    fun copy(): Exercice
    {
        val exercices = Exercice()
        exercices.maxExecutionTime = maxExecutionTime
        exercices.minExecutionTime = minExecutionTime
        exercices.startTimer = startTimer
        exercices.lastTimer = lastTimer
        exercices.numberOfRepetitionToDo = numberOfRepetitionToDo
        exercices.numberOfRepetition = numberOfRepetition
        exercices.numberOfRepetitionReached = numberOfRepetitionReached
        exercices.simultaneousMovement = simultaneousMovement
        exercices.movementList = movementList
        return exercices
    }
}