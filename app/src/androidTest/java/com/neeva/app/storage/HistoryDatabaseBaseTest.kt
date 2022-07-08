package com.neeva.app.storage

import com.neeva.app.BaseHiltTest
import javax.inject.Inject
import org.junit.After

abstract class HistoryDatabaseBaseTest : BaseHiltTest() {
    @Inject lateinit var database: HistoryDatabase

    @After
    open fun tearDown() {
        database.close()
    }
}
