package com.epmus.mobile.program_fragment

import com.epmus.mobile.poseestimation.BodyPart
import com.epmus.mobile.poseestimation.Exercice
import com.epmus.mobile.poseestimation.Movement
import com.epmus.mobile.program_fragment.ExerciceData.Companion.getExerciceData
import java.lang.reflect.Type

class ExerciceData {
    var name: String = ""
    var exercice: Exercice = Exercice()

    companion object {
        fun getExerciceData(exerciceName: ExerciceNameList?): ExerciceData {

            var exercice = Exercice()

            var movement = Movement(
                BodyPart.L_SHOULDER.ordinal,
                BodyPart.L_ELBOW.ordinal,
                BodyPart.L_WRIST.ordinal
            )
            var movement2 = Movement(
                BodyPart.R_SHOULDER.ordinal,
                BodyPart.R_ELBOW.ordinal,
                BodyPart.R_WRIST.ordinal
            )

            exercice.minExecutionTime = 1.0f
            exercice.maxExecutionTime = 3.0f
            exercice.numberOfRepetitionToDo = 5

            if (exerciceName == ExerciceNameList.ExerciceBras) {
                movement.startingAngle = 180
                movement.endingAngle = 90
                movement.isAngleAntiClockWise = true
                movement2.startingAngle = 180
                movement2.endingAngle = 270
                movement2.isAngleAntiClockWise = false
                exercice.movementList.add(movement)
                exercice.movementList.add(movement2)
            } else if (exerciceName == ExerciceNameList.ExerciceBrasGauche) {
                movement.startingAngle = 170
                movement.endingAngle = 90
                movement.isAngleAntiClockWise = true
                exercice.movementList.add(movement)
            } else if (exerciceName == ExerciceNameList.ExerciceBrasDroit) {
                movement2.startingAngle = 180
                movement2.endingAngle = 270
                movement2.isAngleAntiClockWise = false
                exercice.movementList.add(movement2)
            }

            var exerciceData: ExerciceData = ExerciceData()
            exerciceData.exercice = exercice
            exerciceData.name = exerciceName?.exerciceName!!

            return exerciceData
        }
    }
}

enum class ExerciceNameList(val exerciceName: String) {
    ExerciceBrasGauche("Exercice Bras Gauche"),
    ExerciceBrasDroit("Exercice Bras Droit"),
    ExerciceBras("Exercice Bras");

    companion object {
        fun getEnumValue(value: String): ExerciceNameList? = values().find { it.exerciceName == value }
    }
}

enum class ExerciceList(val exerciceName: String, val exercice: ExerciceData) {
    ExerciceBrasGauche(ExerciceBrasGauche.exerciceName, getExerciceData(ExerciceNameList.ExerciceBrasGauche)),
    ExerciceBrasDroit(ExerciceBrasDroit.exerciceName, getExerciceData(ExerciceNameList.ExerciceBrasDroit)),
    ExerciceBras(ExerciceBras.exerciceName, getExerciceData(ExerciceNameList.ExerciceBras))
}