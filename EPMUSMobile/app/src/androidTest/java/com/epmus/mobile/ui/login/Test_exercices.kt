package com.epmus.mobile.ui.login


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
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
import java.lang.Exception

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test_exercices {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Test
    fun test_exercices() {
        try {
            onView(withContentDescription("More options")).check(matches(isDisplayed()))
            onView(withContentDescription("More options")).perform(click())
            onView(withText("Déconnexion")).check(matches(isDisplayed()))
            onView(withText("Déconnexion")).perform(click())
            Thread.sleep(1000);
        }
        catch (e: Exception){

        }
        finally {
            // Connexion
            onView(withId(R.id.login)).check(matches(isDisplayed()))
            onView(withId(R.id.username)).perform(typeText("admin1"))
            onView(withId(R.id.password)).perform(typeText("admin1"))
            onView(withId(R.id.login)).perform(click())
            Thread.sleep(1000);
            // Accès au programme d'exercice
            onView(withId(R.id.activity_program)).check(matches(isDisplayed()))
            onView(withId(R.id.activity_program)).perform(click())
            onView(withId(R.id.fab_messaging)).check(matches(isDisplayed()))
            onView(withId(R.id.program_list)).check(matches(isDisplayed()))
            onView(allOf(isDisplayed(), withId(R.id.playButton)))

            // checks des boutons non fonctionnels // A DELETE
            // onView(withId(R.id.playButton)).check(matches(isDisplayed()))
            // onView(withContentDescription("Play Button")).check(matches(isDisplayed()))
            // onView(allOf(withId(R.id.playButton), withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))).check(matches(isDisplayed()))
            // onData(withId(R.id.playButton)).inAdapterView(withId(R.id.program_list)).atPosition(0).check(matches(isDisplayed()))

            // Accès à la messagerie
            onView(withId(R.id.fab_messaging)).perform(click())
            onView(withId(R.id.recyclerView_newMessage)).check(matches(isDisplayed()))
            pressBack()
            // Accès à un exercice
            onView(withId(R.id.program_list)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
            onView(withId(R.id.program_detail)).check(matches(isDisplayed()))
            onView(withId(R.id.fab_play)).check(matches(isDisplayed()))
            // Deconnexion
            pressBack()
            pressBack()
            onView(withContentDescription("More options")).check(matches(isDisplayed()))
            onView(withContentDescription("More options")).perform(click())
            onView(withText("Déconnexion")).check(matches(isDisplayed()))
            onView(withText("Déconnexion")).perform(click())
            Thread.sleep(1000);
            onView(withId(R.id.login)).check(matches(isDisplayed()))
        }
    }
}

