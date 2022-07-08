package com.neeva.app

import androidx.annotation.CallSuper
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Before
import org.junit.Rule

/** Base class for instrumentation tests.  Subclasses must be annotated with @HiltAndroidTest. */
abstract class BaseHiltTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    @CallSuper
    open fun setUp() {
        hiltRule.inject()
    }
}
