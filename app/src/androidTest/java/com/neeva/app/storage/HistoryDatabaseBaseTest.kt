package com.neeva.app.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.neeva.app.BaseHiltTest
import org.junit.After
import org.junit.Before

abstract class HistoryDatabaseBaseTest : BaseHiltTest() {
    protected lateinit var database: HistoryDatabase

    @Before
    open fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = HistoryDatabase.createInMemory(context)
    }

    @After
    open fun tearDown() {
        database.close()
    }
}
