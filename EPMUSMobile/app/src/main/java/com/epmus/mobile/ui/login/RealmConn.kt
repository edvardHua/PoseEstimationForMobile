package com.epmus.mobile.ui.login

import android.app.Application
import android.util.Log
import com.epmus.mobile.BuildConfig

import io.realm.Realm
import io.realm.log.LogLevel
import io.realm.log.RealmLog
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration

lateinit var realmApp: App

// global Kotlin extension that resolves to the short version
// of the name of the current class. Used for labelling logs.
inline fun <reified T> T.TAG(): String = T::class.java.simpleName

/*
* TaskTracker: Sets up the taskApp Realm App and enables Realm-specific logging in debug mode.
*/
class TaskTracker : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        realmApp = App(
            AppConfiguration.Builder(BuildConfig.MONGODB_REALM_APP_ID)
                .build()
        )

        // Enable more logging in debug mode
        if (BuildConfig.DEBUG) {
            RealmLog.setLevel(LogLevel.ALL)
        }

        Log.v(TAG(), "Initialized the Realm App configuration for: ${realmApp.configuration.appId}")
    }
}