package com.epmus.mobile.ui.login


import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.epmus.mobile.AlertsActivity
import com.epmus.mobile.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test_deconnexion {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Test
    fun test_deconnexion() {
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
            onView(withId(R.id.username)).perform(typeText("admin1"))
            onView(withId(R.id.password)).perform(typeText("admin1"))
            onView(withId(R.id.login)).perform(click())
            Thread.sleep(1000);
            // Deconnexion
            onView(withContentDescription("More options")).check(matches(isDisplayed()))
            onView(withContentDescription("More options")).perform(click())
            onView(withText("Déconnexion")).check(matches(isDisplayed()))
            onView(withText("Déconnexion")).perform(click())
            Thread.sleep(1000);
            onView(withId(R.id.login)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun loginActivityBadUserTest() {
        onView(withId(R.id.username)).perform(typeText("BadUser"))
        onView(withId(R.id.password)).perform(typeText("BadUser"))
        onView(withId(R.id.login)).perform(click())
        // onView(withText("La connexion n'a pas réussi")).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}
