// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.chromium.weblayer.WebMessage
import org.chromium.weblayer.WebMessageCallback
import org.chromium.weblayer.WebMessageReplyProxy

abstract class ContentFilterCallbacks : WebMessageCallback() {
    companion object {
        const val MESSAGE_STARTED = "started"
        const val MESSAGE_PROVIDER_LOG = "provider_log"
        const val MESSAGE_GET_IS_FLAGGED = "is_flagged"
        const val MESSAGE_NOTICE_HANDLED = "notice_handled"
        const val MESSAGE_GET_PREFERENCES = "get_preferences"
        private val TAG = ContentFilterCallbacks::class.simpleName

        private val moshi: Moshi = Moshi.Builder().build()
        private val messageAdapter = moshi.adapter<CookieEngineMessage<Any>>(
            Types.newParameterizedType(CookieEngineMessage::class.java, Any::class.java)
        )

        private val prefsMessageAdapter =
            moshi.adapter<CookieEngineMessage<ContentFilteringPreferences>>(
                Types.newParameterizedType(
                    CookieEngineMessage::class.java,
                    ContentFilteringPreferences::class.java
                )
            )

        private val providerMessageAdapter = moshi.adapter<CookieEngineMessage<String>>(
            Types.newParameterizedType(
                CookieEngineMessage::class.java,
                String::class.java
            )
        )

        private val flagMessageAdapter = moshi.adapter<CookieEngineMessage<Boolean>>(
            Types.newParameterizedType(
                CookieEngineMessage::class.java,
                Boolean::class.javaObjectType
            )
        )
    }

    override fun onWebMessageReceived(replyProxy: WebMessageReplyProxy, message: WebMessage) {
        super.onWebMessageReceived(replyProxy, message)

        // First thing's first: find out which message we got!
        val plainMessage = messageAdapter.fromJson(message.contents)

        when (plainMessage?.type) {
            MESSAGE_PROVIDER_LOG -> handleProviderLog(message.contents)
            MESSAGE_NOTICE_HANDLED -> onNoticeHandled()
            MESSAGE_GET_PREFERENCES -> handleGetPreferences(replyProxy)
            MESSAGE_GET_IS_FLAGGED -> handleIsFlagged(replyProxy)
            MESSAGE_STARTED -> {}
            else -> Log.w(
                TAG,
                "Unexpected message from Cookie Cutter engine: ${plainMessage?.type}"
            )
        }
    }

    private fun handleProviderLog(message: String) {
        // parse as a string message
        val provider = providerMessageAdapter.fromJson(message)?.data
            ?: return
        onLogProvider(provider)
    }

    private fun handleGetPreferences(replyProxy: WebMessageReplyProxy) {
        val prefs = onGetPreferences()
        val response = CookieEngineMessage(MESSAGE_GET_PREFERENCES, prefs)
        sendMessage(replyProxy, response, prefsMessageAdapter)
    }

    private fun handleIsFlagged(replyProxy: WebMessageReplyProxy) {
        val isFlagged = onIsFlagged(replyProxy.sourceOrigin)

        // then reply
        val response = CookieEngineMessage(MESSAGE_GET_IS_FLAGGED, isFlagged)
        sendMessage(replyProxy, response, flagMessageAdapter)
    }

    private fun <T> sendMessage(
        replyProxy: WebMessageReplyProxy,
        message: CookieEngineMessage<T>,
        adapter: JsonAdapter<CookieEngineMessage<T>>
    ) {
        val json = adapter.toJson(message)
        replyProxy.postMessage(WebMessage(json))
    }

    abstract fun onGetPreferences(): ContentFilteringPreferences
    abstract fun onNoticeHandled()
    abstract fun onIsFlagged(origin: String): Boolean
    abstract fun onLogProvider(providerId: String)
}
