package com.epmus.mobile.poseestimation

import android.graphics.PointF
import java.io.Serializable
import java.util.ArrayList
import kotlin.math.*

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

    var initList = ArrayList<ArrayList<PointF>>()
    var notMovingInitList = ArrayList<Boolean>()
    var isInit: Boolean = false
    var notMovingStartTime: Long? = null
    var notMovingTimer: Int = 0


    fun initialisationVerification(drawView: DrawView)
    {
        //For Each body part
        initList.forEachIndexed()
        { index, item ->

            //Calculate average (mean) and standart deviation (ecart type)
            var meanX: Float = -1.0f
            var meanY: Float = -1.0f
            var stdDevX: Float = -1.0f
            var stdDevY: Float = -1.0f
            if (item.count() == drawView.frameCounterMaxInit) {
                //sum
                var totalX: Float = 0.0000f
                var totalY: Float = 0.0000f
                item.forEach()
                {
                    totalX += it.x
                    totalY += it.y
                }

                //mean
                meanX = totalX / item.count()
                meanY = totalY / item.count()

                //Variance
                var varianceX: Float = 0.0000f
                var varianceY: Float = 0.0000f
                item.forEach()
                {
                    var differenceX = it.x - meanX
                    varianceX += (differenceX * differenceX)
                    var differenceY = it.y - meanY
                    varianceY += (differenceY * differenceY)
                }

                //standart deviation
                stdDevX = sqrt(varianceX)
                stdDevY = sqrt(varianceX)
            }


            // Modify list
            var pointX: Float = drawView.mDrawPoint[index].x
            var pointY: Float = drawView.mDrawPoint[index].y
            var pF = PointF(pointX, pointY)
            if (!pointX.isNaN() && !pointY.isNaN() ) {
                if (item.count() == drawView.frameCounterMaxInit) {
                    item.removeAt(0)
                }
                // add only if not 0 (out of frame)
                if (pointX.toInt() != 0 && pointY.toInt() != 0) {
                    item.add(pF)
                }
            }


            //
            if (item.count() == drawView.frameCounterMaxInit) {
                //sum
                var totalX: Float = 0.0000f
                var totalY: Float = 0.0000f
                item.forEach()
                {
                    totalX += it.x
                    totalY += it.y
                }

                //mean
                var avgX = totalX / item.count()
                var avgY = totalY / item.count()

                //If not moving
                notMovingInitList[index] = avgX <= meanX + drawView.nearPointFInit && avgX >= meanX - drawView.nearPointFInit &&
                        avgY <= meanY + drawView.nearPointFInit && avgY >= meanY - drawView.nearPointFInit
            }

        }

        //look if every body part are not moving
        var isNotMoving: Boolean = true
        notMovingInitList.forEach()
        {
            if (it == false) {
                isNotMoving = false
            }
        }

        if (isNotMoving) {
            if (notMovingStartTime == null)
            {
                notMovingStartTime = System.currentTimeMillis()
                notMovingTimer = 5
            }
            else
            {
                var currentTime: Long = System.currentTimeMillis()
                var targetTime: Long = 5000
                notMovingTimer = 5 - ((currentTime - notMovingStartTime!!)/1000).toInt()
                if (currentTime - notMovingStartTime!! >= targetTime)
                {
                    isInit = true
                }
            }
        }
        else {
            notMovingStartTime = null
        }
    }


    fun exerciceVerification(drawView: DrawView)
    {
        movementList.forEach()
        {

            calculateMembersLength(it!!,drawView!!)
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
                        1 -> {it.movementState = 3}
                        2 -> {it.movementState = 4}
                    }
                }
                else
                {
                    when(it!!.movementState)
                    {
                        3 -> {it.movementState = 1}
                        4 -> {it.movementState = 2}
                    }
                }
            }
        }

        if(isRepetitionSimultaneousExerciceDone(movementList) == true)
        {
            movementList.forEach()
            {
                it.movementState = 2
            }
        }

        if(isInStartingPositionSimultaneousExercice(movementList) == true)
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

    fun calculateMembersLength(movement: Movement, drawView: DrawView)
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

        var member1Length = sqrt(X1ToX0.pow(2) + Y1ToY0.pow(2))
        var member2Length = sqrt(X1ToX2.pow(2) + Y1ToY2.pow(2))

        if(! member1Length.isNaN() && ! member2Length.isNaN())
        {
            if(movement.member1LengthLastFrames.size == drawView.frameCounterMax && movement.member2LengthLastFrames.size == drawView.frameCounterMax)
            {
                movement.member1LengthLastFrames.removeAt(0)
                movement.member2LengthLastFrames.removeAt(0)
            }
            movement.member1LengthLastFrames.add(member1Length.toDouble())
            movement.member2LengthLastFrames.add(member2Length.toDouble())
        }

        if(movement.member1LengthLastFrames.size != 0 && movement.member2LengthLastFrames.size != 0)
        {
            movement.member1Length = movement.member1LengthLastFrames.average().roundToInt()
            movement.member2Length = movement.member2LengthLastFrames.average().roundToInt()
        }
    }

    private fun isInStartingPositionSimultaneousExercice(movementList: ArrayList<Movement>): Boolean
    {
        var inStartingPosition = true
        movementList.forEach()
        {
            if(it.movementState != 4)
            {
                inStartingPosition = false
            }
        }
        return inStartingPosition
    }

    fun calculateAngleHorizontalOffset(movement: Movement, drawView: DrawView, bodyPartCenterOfRotation: Int, endBodyPart: Int)
    {
        var bodyPartCenterOfRotationX = drawView.mDrawPoint[bodyPartCenterOfRotation].x
        var bodyPartCenterOfRotationY = drawView.mDrawPoint[bodyPartCenterOfRotation].y

        var endBodyPartX = drawView.mDrawPoint[endBodyPart].x
        var endBodyPartY = drawView.mDrawPoint[endBodyPart].y

        var deltaY = endBodyPartY - bodyPartCenterOfRotationY
        var deltaX = endBodyPartX - bodyPartCenterOfRotationX

        var angleRad: Float = kotlin.math.atan(deltaY/deltaX)
        var angleDeg : Double = ((angleRad*180)/Math.PI)

        //First quadrant
        if(sign(deltaX).toInt() == 1 && sign(deltaY).toInt() == 1)
        {
            //angleDeg = 180 - angleDeg
        }

        //Second quadrant
        else if(sign(deltaX).toInt() == -1 && sign(deltaY).toInt() == 1)
        {
            angleDeg += 180
        }

        //Third quadrant
        else if(sign(deltaX).toInt() == -1 && sign(deltaY).toInt() == -1)
        {
            angleDeg = -1*(180 - angleDeg)
        }

        //Fourth quadrant
        else
        {
            //angleDeg *= -1
        }

        if(! angleDeg.isNaN())
        {
            if(movement.angleOffsetLastFrames.size == drawView.frameCounterMax)
            {
                movement.angleOffsetLastFrames.removeAt(0)
            }
            movement.angleOffsetLastFrames.add(angleDeg)
        }

        if(movement.angleOffsetLastFrames.size != 0)
            movement.angleOffset = movement.angleOffsetLastFrames.average().roundToInt()

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
        if (movement.isAngleAntiClockWise!!)
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
                0,2,4 -> {return movement.angleAvg!! > movement.startingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.startingAngle!! + movement.acceptableAngleVariation}
                1,3 -> {return movement.angleAvg!! > movement.endingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.endingAngle!! + movement.acceptableAngleVariation}
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