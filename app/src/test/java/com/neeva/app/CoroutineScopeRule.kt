package com.neeva.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Manages a CoroutineScope for testing kotlin coroutines and Flows.  The scope is automatically
 * canceled at the end of the test to stop any tasks that may still be running (i.e. Flow
 * collection).
 *
 * You might want to use [kotlinx.coroutines.test.runTest] instead.
 *
 * While the docs say that [TestScope] should be used instead of [TestCoroutineScope], it seems like
 * it ends up swallowing Exceptions or crashing before being able to catch them even when it is used
 * outside of a [runTest] block, resulting in tests failing without explanation.  Keep an eye out
 * for RuntimeExceptions in case your tests this happens.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineScopeRule : TestRule {
    val scope: TestScope = TestScope()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    scope.coroutineContext
                    base?.evaluate()
                } finally {
                    scope.cancel()
                }
            }
        }
    }
}
