// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.net.Uri
import android.util.Base64
import androidx.core.net.toFile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlin.experimental.and

// ref: https://github.com/neevaco/neeva/blob/8907317c2ce8851599baa1ca4267724aa8687665/base/collections/bloom/bloom.go
class BloomFilter {
    /**
     * Filter represents a bloom filter with backing array `data` that will hash each element `k`
     * times.
     */
    @JsonClass(generateAdapter = true)
    data class Filter(
        /** The Bloom Filter, stored as a Base64 encoded ByteArray. */
        @Json(name = "Data") val data: String,
        /** The number of functions used to generate the Bloom Filter. */
        @Json(name = "K") val k: Int
    ) {
        val dataByteArray: ByteArray = Base64.decode(data, Base64.DEFAULT)
        val kUInt: UInt = k.toUInt()
    }

    var filter: Filter? = null
        private set

    /**
     * Supports decoding from a json file with values:
     * {
     *    "Data": <base 64 encoded string for the bits>,
     *    "K": <number of hash functions>
     * }
     */
    fun loadFilter(fileUrl: Uri) {
        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(Filter::class.java)
        filter = jsonAdapter.fromJson(fileUrl.toFile().readText())
    }

    /**
     * Check a given key may be a member of the bloom filter
     * @param key to look for in the bloom filter
     * @return true if this bloom filter might contain `key` and false if the key is conclusively
     * not present.
     */
    fun mayContain(key: String): Boolean {
        filter?.let {
            val keyHash = fnv1aHash(key.toByteArray())
            var h1 = (keyHash shr 31).toUInt()
            val h2 = (keyHash and 0xFFFF_FFFFu).toUInt()
            val numBits = (it.dataByteArray.size * 8).toUInt()
            for (i in (0).toUInt() until it.kUInt) {
                val bit = (h1 % numBits).toInt()
                h1 += h2
                if (it.dataByteArray[(bit / 8)] and (1 shl (bit % 8)).toByte() == (0).toByte()) {
                    return false
                }
            }
            return true
        } ?: return false
    }

    /**
     * This is a variant of fnv-1a with an increased block size to improve performance for larger keys.
     * @return upper and lower bits of the 64-bit result value
     * See: https://en.wikipedia.org/wiki/Fowler–Noll–Vo_hash_function.
     */
    private fun fnv1aHash(bytes: ByteArray): ULong {
        val offset: ULong = 14695981039346656037uL
        val prime: ULong = 1099511628211uL

        var hash = offset
        var i = 0

        while (i < bytes.size - 3) {
            val combined = bytes[i].toULong() or
                (bytes[i + 1].toULong() shl 8) or
                (bytes[i + 2].toULong() shl 16) or
                (bytes[i + 3].toULong() shl 24)
            hash = hash xor combined
            hash *= prime
            i += 4
        }
        while (i < bytes.size) {
            hash = hash xor bytes[i].toULong()
            hash *= prime
            i += 1
        }
        return hash
    }
}
