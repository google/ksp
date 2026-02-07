package com.example.workload_android

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.example.Foo

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        Foo().exampleinstrumentedtest
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.workload_android.test", appContext.packageName)
    }
}