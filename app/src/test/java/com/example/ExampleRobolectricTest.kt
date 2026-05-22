package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.WeatherDashboardScreen
import com.example.ui.WeatherViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nimbus Weather", appName)
    }

    @Test
    fun testDashboardFullFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = WeatherViewModel(app)

        composeTestRule.setContent {
            WeatherDashboardScreen(viewModel = viewModel)
        }

        // Initially we should be on the Google Login screen
        // Perform a simulated click on the Google account button
        composeTestRule.onNodeWithTag("google_signin_button_vasanarun_gmail.com").performClick()

        // Wait for Compose to process state updates (save session to Room database, load weather data, etc.)
        composeTestRule.waitForIdle()
    }
}
