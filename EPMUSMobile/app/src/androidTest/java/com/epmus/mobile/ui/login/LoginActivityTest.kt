package com.epmus.mobile.ui.login

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.epmus.mobile.MainMenuActivity
import com.epmus.mobile.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = IntentsTestRule(LoginActivity::class.java)

    @Test
    fun loginActivityGoodUserTest() {
        //Only works with emulateur
        onView(withId(R.id.username)).perform(typeText("admin1"))
        onView(withId(R.id.password)).perform(typeText("admin1"))
        onView(withId(R.id.login)).perform(click())
        //Temporary sleep until proper solution is found
        Thread.sleep(500);
        intended(hasComponent(MainMenuActivity::class.java.name))
    }

    @Test
    fun loginActivityBadUserTest() {
        onView(withId(R.id.username)).perform(typeText("BadUser"))
        onView(withId(R.id.password)).perform(typeText("BadUser"))
        onView(withId(R.id.login)).perform(click())
    }
}