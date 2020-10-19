package com.epmus.mobile.ui.login


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.epmus.mobile.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test_exercices {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Test
    fun test_exercices() {

        // Connexion
        onView(withId(R.id.username)).perform(typeText("admin1"))
        onView(withId(R.id.password)).perform(typeText("admin1"))
        onView(withId(R.id.login)).perform(click())
        Thread.sleep(1000);
        // Accès au programme d'exercice
        onView(withId(R.id.activity_program)).check(matches(isDisplayed()))
        onView(withId(R.id.activity_program)).perform(click())
        onView(withId(R.id.program_list)).check(matches(isDisplayed()))
        onView(withId(R.id.fab_messaging)).check(matches(isDisplayed()))
        // Accès à la messagerie
        onView(withId(R.id.fab_messaging)).perform(click())
        onView(withId(R.id.recyclerView_newMessage)).check(matches(isDisplayed()))
        pressBack()
        // Accès à un exercice
        onView(withId(R.id.program_list)).perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        onView(withId(R.id.program_detail)).check(matches(isDisplayed()))
        onView(withId(R.id.fab_play)).check(matches(isDisplayed()))
    }
}
