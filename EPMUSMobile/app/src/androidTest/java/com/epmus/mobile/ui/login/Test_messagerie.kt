package com.epmus.mobile.ui.login


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
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
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Test_messagerie {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    @Test
    fun test_messagerie() {
        // Connexion
        onView(withId(R.id.username)).perform(typeText("admin1"))
        onView(withId(R.id.password)).perform(typeText("admin1"))
        onView(withId(R.id.login)).perform(click())
        Thread.sleep(1000);
        // Accès au chat + envoit d'un message
        onView(withId(R.id.activity_messaging)).check(matches(isDisplayed()))
        onView(withId(R.id.activity_messaging)).perform(click())
        onView(withId(R.id.recyclerView_newMessage)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView_newMessage)).perform(actionOnItemAtPosition<ViewHolder>(0, click()))
        onView(withId(R.id.send_button_chat_log)).check(matches(isDisplayed()))
        onView(withId(R.id.editText_chat_log)).perform(typeText("test"))
        // Bouton envoyer pas encore fonctionnel
        onView(withId(R.id.send_button_chat_log)).perform(click())
    }
}
