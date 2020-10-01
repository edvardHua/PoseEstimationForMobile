package com.epmus.mobile.program_fragment

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.epmus.mobile.AccountActivity
import com.epmus.mobile.HistoryActivity
import com.epmus.mobile.R
import com.epmus.mobile.SettingsActivity
import com.epmus.mobile.poseestimation.BodyPart
import com.epmus.mobile.poseestimation.CameraActivity
import com.epmus.mobile.poseestimation.Exercice
import com.epmus.mobile.poseestimation.Movement
import com.epmus.mobile.ui.login.LoginActivity

class ProgramActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_program)

        // toolbar support
        setSupportActionBar(findViewById(R.id.my_toolbar_test))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_account -> {
            intent = Intent(this, CameraActivity::class.java)
            // TODO: Remove this, temp
            var movement = Movement(BodyPart.L_SHOULDER.ordinal, BodyPart.L_ELBOW.ordinal, BodyPart.L_WRIST.ordinal)
            var movement2 = Movement(BodyPart.R_SHOULDER.ordinal, BodyPart.R_ELBOW.ordinal, BodyPart.R_WRIST.ordinal)
            movement.startingAngle = 180
            movement.endingAngle = 90
            movement.isAngleAntiClockWise = true
            movement2.startingAngle = 180
            movement2.endingAngle = 270
            movement2.isAngleAntiClockWise = false
            var exercice = Exercice()
            exercice.minExecutionTime = 1.0f
            exercice.maxExecutionTime = 3.0f
            exercice.simultaneousMovement = true
            exercice.numberOfRepetitionToDo = 5
            exercice.movementList.add(movement)
            exercice.movementList.add(movement2)
            intent.putExtra("exercice", exercice)
            startActivity(intent)
            /*val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)*/
            true
        }

        R.id.action_history -> {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
            true
        }

        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        R.id.action_logout -> {
            // TODO: handle logout properly
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}
