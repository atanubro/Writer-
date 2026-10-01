package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Office Letter AI", appName)
  }

  @Test
  fun testSpeechToTextManager_intentCreation() {
    val intent = com.example.data.speech.SpeechToTextManager.createSpeechIntent(
        prompt = "Dictate letter",
        languageTag = "en-US"
    )
    org.junit.Assert.assertNotNull(intent)
    assertEquals(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
    assertEquals("en-US", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE))
    assertEquals("Dictate letter", intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_PROMPT))
  }
}
