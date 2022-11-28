// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.publicsuffixlist

import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.neeva.app.LoadingState
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.IDN
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Derives the registerable domain name from the provided domain.
 * See https://publicsuffix.org/list/ for information about how this should work.
 */
class DomainProviderImpl(val context: Context) : DomainProvider {
    companion object {
        const val SUFFIX_FILENAME = "public_suffix_list.dat"
    }

    private val trieRoot = HostTrieNode()
    private val exceptionRules = mutableListOf<String>()

    private val _loadingState = MutableStateFlow(LoadingState.UNINITIALIZED)
    val loadingState: StateFlow<LoadingState> = _loadingState

    @WorkerThread
    internal suspend fun initialize() {
        if (!_loadingState.compareAndSet(LoadingState.UNINITIALIZED, LoadingState.LOADING)) return
        loadList(context)
    }

    @WorkerThread
    private suspend fun loadList(context: Context) {
        var inputStream: InputStream? = null
        var streamReader: InputStreamReader? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = context.assets.open(SUFFIX_FILENAME)
            streamReader = InputStreamReader(inputStream)
            bufferedReader = BufferedReader(streamReader)

            bufferedReader.lines().forEach { listEntry ->
                when {
                    listEntry.isBlank() || listEntry.startsWith("//") -> {}

                    listEntry.startsWith("!") -> {
                        exceptionRules.add(listEntry.drop(1))
                    }

                    else -> {
                        // Store the Punycoded versions for comparison.
                        val pieces = listEntry.split(".").map { IDN.toASCII(it) }
                        trieRoot.add(pieces)
                    }
                }
            }

            _loadingState.value = LoadingState.READY
        } catch (e: Exception) {
            Timber.e("Failed to load suffix list", e)
        } finally {
            bufferedReader?.close()
            streamReader?.close()
            inputStream?.close()
        }
    }

    /** Returns the registerable domain for the given [uri], or null if it is invalid. */
    override fun getRegisteredDomain(uri: Uri?): String? {
        uri ?: return null
        return if (uri.scheme == null) {
            // The user might be typing something in manually.
            getRegisteredDomainForHost(uri.toString().split("/").firstOrNull())
        } else {
            getRegisteredDomainForHost(uri.host)
        }
    }

    /** Returns the registerable domain for the given [hostname], or null if it is invalid. */
    fun getRegisteredDomainForHost(hostname: String?): String? {
        val sanitizedHostname = hostname?.lowercase() ?: return null
        if (sanitizedHostname.isEmpty()) return null

        val labels = sanitizedHostname.split(".")
        labels.forEach { if (it.isEmpty()) return null }

        // Too short: Toss everything.
        if (labels.size < 2) return null

        // Unlisted TLD: Take the last two parts of the sanitized host name.
        if (!trieRoot.hasChild(labels.last())) {
            return labels.takeLast(2).joinToString(".")
        }

        // Exception rule: Always takes precedence.
        // 3. If more than one rule matches, the prevailing rule is the one which is an exception rule.
        var longestExceptionRuleLabels: List<String> = emptyList()
        exceptionRules.forEach { currentRule ->
            if (!sanitizedHostname.endsWith(currentRule)) return@forEach

            val currentRuleLabels = currentRule.split(".")
            if (currentRuleLabels.size > longestExceptionRuleLabels.size) {
                longestExceptionRuleLabels = currentRuleLabels
            }
        }
        if (longestExceptionRuleLabels.isNotEmpty()) {
            // 5. If the prevailing rule is a exception rule, modify it by removing the leftmost label.
            return if (labels.size > longestExceptionRuleLabels.size) {
                labels.drop(1).joinToString(".")
            } else {
                labels.joinToString(".")
            }
        }

        // 4. If there is no matching exception rule, the prevailing rule is the one with the most labels.
        val resultPath = trieRoot.findLongestSuffix(labels)
        return if (resultPath.size == labels.size) {
            // Filter out strings that perfectly match a suffix because we need one more label.
            null
        } else {
            // 6. The public suffix is the set of labels from the domain which match the labels of
            //    the prevailing rule, using the matching algorithm above.
            // 7. The registered or registrable domain is the public suffix plus one additional label.
            val publicSuffix = resultPath.joinToString(".")
            val uniquePartIndex = labels.size - 1 - resultPath.size
            val label = labels.getOrNull(uniquePartIndex) ?: return null
            return "$label.$publicSuffix"
        }
    }

    /** Maintains a trie that determines the longest Public Suffix applicable to a given domain. */
    private class HostTrieNode {
        private val children: MutableMap<String, HostTrieNode> = mutableMapOf()

        fun hasChild(label: String) = children.containsKey(label)

        /** Appends the given labels as descendants of this [HostTrieNode]. */
        fun add(labels: List<String>) {
            if (labels.isEmpty()) return
            val currentLabel = labels.last()
            val remainingLabels = labels.dropLast(1)

            val child = children[currentLabel] ?: HostTrieNode()
            child.add(remainingLabels)
            children[currentLabel] = child
        }

        /** Finds the longest suffix possible that fits the given [labels]. */
        fun findLongestSuffix(labels: List<String>): List<String> {
            val suffixPieces = mutableListOf<String>()
            findLongestSuffix(labels, suffixPieces)
            return suffixPieces
        }

        private fun findLongestSuffix(labels: List<String>, suffixPieces: MutableList<String>) {
            if (labels.isEmpty()) return
            val currentLabel = labels.last()
            val remainingLabels = labels.dropLast(1)

            val punycodedPiece = IDN.toASCII(currentLabel)
            val child = children[punycodedPiece] ?: children["*"]
            if (child != null) {
                // Although comparisons must be done using Punycoding, we should use the original
                // when building the path.
                suffixPieces.add(0, currentLabel)
                child.findLongestSuffix(remainingLabels, suffixPieces)
            }
        }
    }
}
