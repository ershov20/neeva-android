package com.neeva.app

import androidx.annotation.CallSuper
import org.junit.After
import org.junit.Before
import org.mockito.Mockito
import org.mockito.MockitoSession
import org.mockito.quality.Strictness

/** Base class for tests that rely on Mockito annotations for automatically creating mocks. */
abstract class BaseTest {
    lateinit var mockitoSession: MockitoSession

    @Before
    @CallSuper
    open fun setUp() {
        mockitoSession = Mockito.mockitoSession()
            .initMocks(this)
            .strictness(Strictness.WARN)
            .startMocking()
    }

    @After
    open fun tearDown() {
        mockitoSession.finishMocking()
    }
}
