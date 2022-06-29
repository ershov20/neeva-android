package com.neeva.app

import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule

/** Base class for instrumentation tests.  Subclasses must be annotated with @HiltAndroidTest. */
abstract class BaseHiltTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
}
