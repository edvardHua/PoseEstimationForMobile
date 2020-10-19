package com.epmus.mobile.poseestimation

import android.graphics.PointF
import android.widget.Chronometer
import java.io.Serializable
import java.util.ArrayList
import kotlin.math.*

class Exercice : Serializable {
    // add to fun .copy() if there is a modif
    var maxExecutionTime: Float? = null
    var minExecutionTime: Float? = null
    var mouvementStartTimer: Long? = null
    var mouvementSpeedTime: Float? = null

    var numberOfRepetitionToDo: Int? = null
    var numberOfRepetition: Int = 0
    var exitStateReached: Boolean = false
    var numberOfRepetitionReachedTimer: Long? = null

    var movementList = ArrayList<Movement>()

    var initStartTimer: Long? = null
    var initList = ArrayList<ArrayList<PointF>>()
    var notMovingInitList = ArrayList<Boolean>()
    var isInit: Boolean = false
    var initDoneTimer: Long? = null
    var notMovingStartTime: Long? = null
    var notMovingTimer: Int = 0
    var targetTime: Long = 4000
    var stdMax: Int = 100

    var exerciceType: ExerciceType? = null

    //Variable for type CHRONO
    var chronoTime: Int? = 0
    var allowedTimeForExercice: Int? = null
    var exerciceStartTime: Long? = null

    //Variable for type HOLD
    var targetHoldTime: Int? = null
    var holdTime: Long = 0.toLong()
    var wasHolding: Boolean = false
    var isHolding: Boolean = false
    var holdingStartTime: Long? = null
    var currentHoldTime: Long = 0

    //This is used to make sure that a warning cannot be spammed
    var warningCanBeDisplayed: Boolean = true

    fun initialisationVerification(drawView: DrawView) {
        //For Each body part
        initList.forEachIndexed()
        { index, item ->



            // Modify list
            var pointX: Float = drawView.mDrawPoint[index].x
            var pointY: Float = drawView.mDrawPoint[index].y
            var pF = PointF(pointX, pointY)
            if (!pointX.isNaN() && !pointY.isNaN()) {
                if (item.count() == drawView.frameCounterMaxInit) {
                    item.removeAt(0)
                }
                // add only if not 0 (out of frame)
                if (pointX.toInt() != 0 && pointY.toInt() != 0) {
                    item.add(pF)
                }
            }

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

                //if std is below max, target is not moving
                notMovingInitList[index] = stdDevX <= stdMax && stdDevY <= stdMax
            }
        }

        //save the start of the initialization
        if (initStartTimer == null) {
            initStartTimer = System.currentTimeMillis()
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
            if (notMovingStartTime == null) {
                notMovingStartTime = System.currentTimeMillis()
                notMovingTimer = 5
            } else {
                var currentTime: Long = System.currentTimeMillis()
                notMovingTimer =
                    targetTime.toInt() / 1000 - ((currentTime - notMovingStartTime!!) / 1000).toInt()
                if (currentTime - notMovingStartTime!! >= targetTime) {
                    isInit = true
                    initDoneTimer = System.currentTimeMillis()
                }
            }
        } else {
            notMovingStartTime = null
        }
    }

    // Verify the state in which every movement is for the given exercice
    fun exerciceVerification(drawView: DrawView) {
        when (this.exerciceType) {
            ExerciceType.CHRONO -> exerciceVerificationChrono(drawView)
            ExerciceType.REPETITION -> exerciceVerificationRepetition(drawView)
            ExerciceType.HOLD -> exerciceVerificationHold(drawView)
            else -> {}
        }
    }

    //Verify the state for an exercice type in CHRONO
    fun exerciceVerificationChrono(drawView: DrawView) {
        //Sets the start time of the exercice if not started
        if (exerciceStartTime == null) {
            exerciceStartTime = System.currentTimeMillis() / 1000
        }

        movementList.forEach()
        {

            //Calculate new values for this frame
            calculateMembersLength(it, drawView)
            calculateAngleV2(it, drawView)

            //Sets new state for movement according to if the angle is matching or not
            if (isAngleMatching(it)) {
                when (it.movementState) {
                    MovementState.INIT -> {
                        it.movementState = MovementState.STARTING_ANGLE_REACHED
                        mouvementStartTimer = System.currentTimeMillis()
                    }
                    MovementState.STARTING_ANGLE_REACHED -> {
                        it.movementState = MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE
                    }
                    MovementState.ENDING_ANGLE_REACHED -> {
                        it.movementState = MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE
                    }
                }
            } else {
                when (it.movementState) {
                    MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE -> {
                        it.movementState = MovementState.STARTING_ANGLE_REACHED
                    }
                    MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE -> {
                        it.movementState = MovementState.ENDING_ANGLE_REACHED
                    }
                }
            }
        }

        //Verifies if repetition is done and changes state of movements to ENDING_ANGLE_REACHED
        if (isRepetitionSimultaneousExerciceDone(movementList)) {
            movementList.forEach()
            {
                it.movementState = MovementState.ENDING_ANGLE_REACHED
            }
            numberOfRepetition++
            warningCanBeDisplayed = true
            mouvementSpeedTime = calculateTime()
        }

        //Verifies if each movement is at startingAngle is done and changes state of movements to ENDING_ANGLE_REACHED
        if (isInStartingPositionSimultaneousExercice(movementList)) {
            movementList.forEach()
            {
                it.movementState = MovementState.STARTING_ANGLE_REACHED
            }
        }

        //Calculates remaining chrono time
        var currentTime = System.currentTimeMillis() / 1000
        chronoTime = (currentTime - exerciceStartTime!!).toInt()
        chronoTime = allowedTimeForExercice!! - chronoTime!!

        //If no time is left, then the exercice is done
        if (chronoTime!! == 0) {
            exitStateReached = true
        }
    }

    fun exerciceVerificationHold(drawView: DrawView) {
        //Sets the start time of the exercice if not started
        if (exerciceStartTime == null) {
            exerciceStartTime = System.currentTimeMillis() / 1000
        }

        movementList.forEach()
        {

            //Sets initial value of movement state to STARTING_ANGLE_REACHED since startingAngle is not used for this exercice type
            if (it.movementState == MovementState.INIT) {
                it.movementState = MovementState.STARTING_ANGLE_REACHED
            }

            //Calculate new values for this frame
            calculateMembersLength(it, drawView)
            calculateAngleV2(it, drawView)

            //Sets new state for movement according to if the angle is matching or not
            if (isAngleMatching(it)) {
                when (it.movementState) {
                    MovementState.STARTING_ANGLE_REACHED -> {
                        it.movementState = MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE
                        mouvementStartTimer = System.currentTimeMillis()
                    }
                }
            } else {
                when (it.movementState) {
                    MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE -> {
                        it.movementState = MovementState.STARTING_ANGLE_REACHED
                        holdTime += currentHoldTime
                        currentHoldTime = 0
                        wasHolding = false
                        warningCanBeDisplayed = true
                    }
                }
            }
        }

        //Verify if the patient was not holding the correct position and is now holding to set the holdingStartTime
        isInHoldPosition(movementList)
        if (isHolding && !wasHolding) {
            holdingStartTime = System.currentTimeMillis()
            wasHolding = true
            warningCanBeDisplayed = true
        }

        //Verify if the targetHoldTime is reached and set exit state to true
        if (holdingStartTime != null && isHolding) {
            currentHoldTime = System.currentTimeMillis() - holdingStartTime!!

            if (((holdTime + currentHoldTime) / 1000).toInt() >= targetHoldTime!!) {
                exitStateReached = true
                numberOfRepetitionReachedTimer = System.currentTimeMillis()
                holdTime += currentHoldTime
            }
        }
    }

    //Verify if every movement is at state WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE
    private fun isInHoldPosition(movementList: ArrayList<Movement>) {
        var isHolding = true
        movementList.forEach()
        {
            if (it.movementState != MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE) {
                isHolding = false
            }
        }
        this.isHolding = isHolding
    }

    fun exerciceVerificationRepetition(drawView: DrawView) {
        movementList.forEach()
        {

            //Calculate new values for this frame
            calculateMembersLength(it, drawView)
            calculateAngleV2(it, drawView)

            //Sets new state for movement according to if the angle is matching or not
            if (isAngleMatching(it)) {
                when (it.movementState) {
                    MovementState.INIT -> {
                        it.movementState = MovementState.STARTING_ANGLE_REACHED
                        mouvementStartTimer = System.currentTimeMillis()
                    }
                    MovementState.STARTING_ANGLE_REACHED -> {
                        it.movementState = MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE
                    }
                    MovementState.ENDING_ANGLE_REACHED -> {
                        it.movementState = MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE
                    }
                }
            } else {
                when (it.movementState) {
                    MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE -> {
                        it.movementState = MovementState.STARTING_ANGLE_REACHED
                    }
                    MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE -> {
                        it.movementState = MovementState.ENDING_ANGLE_REACHED
                    }
                }
            }
        }

        //Verifies if repetition is done and changes state of movements to ENDING_ANGLE_REACHED
        if (isRepetitionSimultaneousExerciceDone(movementList)) {
            movementList.forEach()
            {
                it.movementState = MovementState.ENDING_ANGLE_REACHED
            }
            numberOfRepetition++
            warningCanBeDisplayed = true
            mouvementSpeedTime = calculateTime()
        }

        //Verifies if each movement is at startingAngle is done and changes state of movements to ENDING_ANGLE_REACHED
        if (isInStartingPositionSimultaneousExercice(movementList)) {
            movementList.forEach()
            {
                it.movementState = MovementState.STARTING_ANGLE_REACHED
            }
        }

        //Verify if the number of repetition is reached and sets exit value to true
        if (numberOfRepetitionToDo != null) {
            if (numberOfRepetitionToDo == numberOfRepetition) {
                exitStateReached = true
                numberOfRepetitionReachedTimer = System.currentTimeMillis()
            }
        }
    }

    //Calculates the length of member1 and member2 for a given movement
    fun calculateMembersLength(movement: Movement, drawView: DrawView) {
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

        if (!member1Length.isNaN() && !member2Length.isNaN()) {
            if (movement.member1LengthLastFrames.size == drawView.frameCounterMax && movement.member2LengthLastFrames.size == drawView.frameCounterMax) {
                movement.member1LengthLastFrames.removeAt(0)
                movement.member2LengthLastFrames.removeAt(0)
            }
            movement.member1LengthLastFrames.add(member1Length.toDouble())
            movement.member2LengthLastFrames.add(member2Length.toDouble())
        }

        if (movement.member1LengthLastFrames.size != 0 && movement.member2LengthLastFrames.size != 0) {
            movement.member1Length = movement.member1LengthLastFrames.average().roundToInt()
            movement.member2Length = movement.member2LengthLastFrames.average().roundToInt()
        }
    }

    //Verify if every movement for a given exercice is in state WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE
    private fun isInStartingPositionSimultaneousExercice(movementList: ArrayList<Movement>): Boolean {
        var inStartingPosition = true
        movementList.forEach()
        {
            if (it.movementState != MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE) {
                inStartingPosition = false
            }
        }
        return inStartingPosition
    }

    fun calculateAngleHorizontalOffset(
        movement: Movement,
        drawView: DrawView,
        bodyPartCenterOfRotation: Int,
        endBodyPart: Int
    ) {
        var bodyPartCenterOfRotationX = drawView.mDrawPoint[bodyPartCenterOfRotation].x
        var bodyPartCenterOfRotationY = drawView.mDrawPoint[bodyPartCenterOfRotation].y

        var endBodyPartX = drawView.mDrawPoint[endBodyPart].x
        var endBodyPartY = drawView.mDrawPoint[endBodyPart].y

        var deltaY = endBodyPartY - bodyPartCenterOfRotationY
        var deltaX = endBodyPartX - bodyPartCenterOfRotationX

        var angleRad: Float = atan(deltaY / deltaX)
        var angleDeg: Double = ((angleRad * 180) / Math.PI)

        //First quadrant
        if (sign(deltaX).toInt() == 1 && sign(deltaY).toInt() == 1) {

        }

        //Second quadrant
        else if (sign(deltaX).toInt() == -1 && sign(deltaY).toInt() == 1) {
            angleDeg += 180
        }

        //Third quadrant
        else if (sign(deltaX).toInt() == -1 && sign(deltaY).toInt() == -1) {
            angleDeg = -1 * (180 - angleDeg)
        }

        //Fourth quadrant
        else {

        }

        if (!angleDeg.isNaN()) {
            if (movement.angleOffsetLastFrames.size == drawView.frameCounterMax) {
                movement.angleOffsetLastFrames.removeAt(0)
            }
            movement.angleOffsetLastFrames.add(angleDeg)
        }

        if (movement.angleOffsetLastFrames.size != 0)
            movement.angleOffset = movement.angleOffsetLastFrames.average().roundToInt()

    }

    //Verify if every movement for a given exercice is in state WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE
    private fun isRepetitionSimultaneousExerciceDone(movementList: ArrayList<Movement>): Boolean {
        var repetitionDone = true
        movementList.forEach()
        {
            if (it.movementState != MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE) {
                repetitionDone = false
            }
        }
        return repetitionDone
    }

    //Calculates the angle between the three points in a movement
    fun calculateAngleV2(movement: Movement, drawView: DrawView) {
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

        var X1X0mod: Float = sqrt((X1ToX0 * X1ToX0) + (Y1ToY0 * Y1ToY0))
        var X1X2mod: Float = sqrt((X1ToX2 * X1ToX2) + (Y1ToY2 * Y1ToY2))

        var vectorProduct: Float = X1ToX0 * X1ToX2 + Y1ToY0 * Y1ToY2

        var angleRad: Float = acos(vectorProduct / (X1X0mod * X1X2mod))
        var angleDeg: Double = ((angleRad * 180) / Math.PI)

        //Adding anti/clockwise effect
        var a = Y1ToY0 / X1ToX0
        var b = pointY0 - (a * pointX0)
        var tmpPointY2 = (a * pointX2) + b
        if (movement.isAngleAntiClockWise!!) {
            if (tmpPointY2 < pointY2) {
                angleDeg = 360 - angleDeg
            }
        } else {
            if (tmpPointY2 > pointY2) {
                angleDeg = 360 - angleDeg
            }
        }


        if (!angleDeg.isNaN()) {
            if (movement.angleValuesLastFrames.size == drawView.frameCounterMax) {
                movement.angleValuesLastFrames.removeAt(0)
            }
            movement.angleValuesLastFrames.add(angleDeg)
        }

        if (movement.angleValuesLastFrames.size != 0)
            movement.angleAvg = movement.angleValuesLastFrames.average().roundToInt()

    }


    //Verify if the angle is matching according to the state of the movement
    fun isAngleMatching(movement: Movement): Boolean {
        if (movement.angleAvg != null) {
            when (movement.movementState) {
                MovementState.INIT, MovementState.ENDING_ANGLE_REACHED, MovementState.WAITING_FOR_OTHER_MOVEMENT_STARTING_ANGLE -> {
                    return movement.angleAvg!! > movement.startingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.startingAngle!! + movement.acceptableAngleVariation
                }
                MovementState.STARTING_ANGLE_REACHED, MovementState.WAITING_FOR_OTHER_MOVEMENT_ENDING_ANGLE -> {
                    return movement.angleAvg!! > movement.endingAngle!! - movement.acceptableAngleVariation && movement.angleAvg!! < movement.endingAngle!! + movement.acceptableAngleVariation
                }
                else -> {
                    return false
                }
            }
        } else {
            return false
        }
    }

    private fun calculateTime(): Float? {
        var timeInSecond: Float? = null
        if (mouvementStartTimer != null) {
            timeInSecond = ((System.currentTimeMillis() - mouvementStartTimer!!).toFloat()) / 1000
        }
        mouvementStartTimer = System.currentTimeMillis()
        return timeInSecond
    }

    fun copy(): Exercice {
        val exercices = Exercice()
        exercices.maxExecutionTime = maxExecutionTime
        exercices.minExecutionTime = minExecutionTime
        exercices.mouvementStartTimer = mouvementStartTimer
        exercices.mouvementSpeedTime = mouvementSpeedTime
        exercices.numberOfRepetitionToDo = numberOfRepetitionToDo
        exercices.numberOfRepetition = numberOfRepetition
        exercices.exitStateReached = exitStateReached
        exercices.numberOfRepetitionReachedTimer = numberOfRepetitionReachedTimer
        exercices.movementList = movementList
        exercices.initList = initList
        exercices.notMovingInitList = notMovingInitList
        exercices.isInit = isInit
        exercices.initDoneTimer = initDoneTimer
        exercices.notMovingStartTime = notMovingStartTime
        exercices.notMovingTimer = notMovingTimer
        exercices.initStartTimer = initStartTimer
        exercices.targetTime = targetTime
        return exercices
    }
}


