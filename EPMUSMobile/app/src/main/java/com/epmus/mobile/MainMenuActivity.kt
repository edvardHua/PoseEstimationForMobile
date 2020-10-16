package com.epmus.mobile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import com.epmus.mobile.Messaging.NewMessageActivity
import com.epmus.mobile.program.ProgramListActivity
import com.epmus.mobile.ui.login.LoginActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // toolbar support
        setSupportActionBar(findViewById(R.id.my_toolbar))

        val program = findViewById<Button>(R.id.activity_program)
        val statistics = findViewById<Button>(R.id.activity_statistics)
        val messaging = findViewById<Button>(R.id.activity_messaging)
        val alerts = findViewById<Button>(R.id.activity_alerts)

        program.setOnClickListener {
            val intent = Intent(this, ProgramListActivity::class.java)
            startActivity(intent)
        }

        statistics.setOnClickListener {
            val intent = Intent(this, StatisticsActivity::class.java)
            startActivity(intent)
        }

        messaging.setOnClickListener {
            val intent = Intent(this, NewMessageActivity::class.java)
            startActivity(intent)
        }

        alerts.setOnClickListener {
            val intent = Intent(this, AlertsActivity::class.java)
            startActivity(intent)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_actions, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_account -> {
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
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
