// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.findinpage

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.chromium.weblayer.FindInPageCallback
import org.chromium.weblayer.FindInPageController
import org.chromium.weblayer.Tab
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FindInPageModelImplTest : BaseTest() {

    private lateinit var findInPageController: FindInPageController
    private lateinit var tab: Tab

    private lateinit var model: FindInPageModelImpl

    override fun setUp() {
        super.setUp()

        findInPageController = mock()
        tab = mock {
            on { getFindInPageController() } doReturn findInPageController
        }

        model = FindInPageModelImpl()
    }

    @Test
    fun findInPage_textIsNullByDefault() {
        expectThat(model.findInPageInfoFlow.value).isEqualTo(FindInPageInfo())
    }

    @Test
    fun findInPage_whenNotShown_textIsNullWithNonNullInput() {
        // Skip calling model.showFindInPage(tab).
        model.updateFindInPageQuery("test")
        expectThat(model.findInPageInfoFlow.value).isEqualTo(FindInPageInfo())
    }

    @Test
    fun findInPage_whenShown_textIsEmpty() {
        model.showFindInPage(tab)
        expectThat(model.findInPageInfoFlow.value).isEqualTo(FindInPageInfo(text = ""))
    }

    @Test
    fun findInPage_whenShownWithTab_textIsPropagated() {
        verify(findInPageController, times(0)).setFindInPageCallback(any())

        model.showFindInPage(tab)

        val findInPageCallback = argumentCaptor<FindInPageCallback>()
        verify(findInPageController, times(1))
            .setFindInPageCallback(findInPageCallback.capture())

        val query = "test"
        model.showFindInPage(tab)
        model.updateFindInPageQuery(query)
        verify(tab.findInPageController, times(1)).find(query, true)
        expectThat(model.findInPageInfoFlow.value.text).isEqualTo(query)
    }

    @Test
    fun findInPage_whenShownWithTab_callbackUpdatesInfo() {
        model.showFindInPage(tab)

        val findInPageCallback = argumentCaptor<FindInPageCallback>()
        verify(findInPageController, times(1))
            .setFindInPageCallback(findInPageCallback.capture())

        findInPageCallback.lastValue.onFindResult(5, 2, false)
        expectThat(model.findInPageInfoFlow.value).isEqualTo(FindInPageInfo("", 2, 5, false))
    }
}
