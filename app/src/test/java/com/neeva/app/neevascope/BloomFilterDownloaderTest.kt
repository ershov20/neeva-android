// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.neevascope

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BloomFilterDownloaderTest : BaseTest() {
    @get:Rule val coroutineScopeRule = CoroutineScopeRule()

    private lateinit var appContext: Context
    private lateinit var fileDir: File

    private lateinit var server: MockWebServer
    private lateinit var serverUrl: String

    private lateinit var bloomFilter: BloomFilter
    private lateinit var bloomFilterDownloader: BloomFilterDownloader

    override fun setUp() {
        super.setUp()

        appContext = ApplicationProvider.getApplicationContext()
        fileDir = appContext.cacheDir

        bloomFilter = BloomFilter()
    }

    @Test
    fun loadChecksumFromFile() {
        val checksumFile = File("src/test/resources/neevascope/valid_checksum.json")

        val checksum = Checksum.loadChecksum(checksumFile.readText())
        expectThat(checksum).isNotNull()
    }

    @Test
    fun calculateChecksumFromFile() {
        val binFile = File("src/test/resources/neevascope/reddit.bin")
        val fileUri = binFile.toUri()

        val fileSha = Checksum.getChecksumFromFile(
            MessageDigest.getInstance("SHA-256"),
            fileUri
        )
        val fileMd5 = Checksum.getChecksumFromFile(
            MessageDigest.getInstance("MD5"),
            fileUri
        )

        expectThat(fileSha)
            .isEqualTo("9b5451084d26e9869a4f372bb3d2ab149d090424b30205658be292794b7ed608")
        expectThat(fileMd5).isEqualTo("70d2143e042de8b27000450144bf2ea1")
    }

    private fun initializeServer(checksumResponse: String, bloomFilterResponse: String) {
        server = MockWebServer()

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/test/bloomfilter/checksum" -> MockResponse().setBody(checksumResponse)
                    "/test/bloomfilter/filter" -> MockResponse().setBody(bloomFilterResponse)
                    else -> MockResponse().setResponseCode(400)
                }
            }
        }
        server.dispatcher = dispatcher

        server.start()
        serverUrl = server.url("/test/bloomfilter").toString()
    }

    @Test
    fun downloadFile_withValidResponse_loadsBloomFilter() {
        val validChecksumFile = File("src/test/resources/neevascope/valid_checksum.json")
        val binFile = File("src/test/resources/neevascope/reddit.bin")

        initializeServer(validChecksumFile.readText(), binFile.readText())

        val file = File(fileDir, "bloomfilter")
        bloomFilterDownloader = BloomFilterDownloader(
            URL("$serverUrl/checksum"),
            URL("$serverUrl/filter"),
            Uri.fromFile(file)
        )

        runBlocking {
            bloomFilterDownloader.download()
        }

        // Confirm that we hit the right endpoint.
        val checksumRequest = server.takeRequest()
        expectThat(checksumRequest.requestUrl?.toString()).isEqualTo("$serverUrl/checksum")
        val filterRequest = server.takeRequest()
        expectThat(filterRequest.requestUrl?.toString()).isEqualTo("$serverUrl/filter")

        // The initial bloom filter should be null.
        expectThat(bloomFilter.filter).isNull()

        expectThat(file.exists()).isTrue()
        // The bloom filter is loaded from downloaded filter file.
        bloomFilter.loadFilter(Uri.fromFile(file))

        // The bloom filter should not be null.
        expectThat(bloomFilter.filter).isNotNull()
    }

    @Test
    fun downloadFile_withInvalidResponse_doesNothing() {
        val invalidChecksumFile = File("src/test/resources/neevascope/invalid_checksum.json")
        val binFile = File("src/test/resources/neevascope/reddit.bin")

        initializeServer(invalidChecksumFile.readText(), binFile.readText())

        val file = File(fileDir, "bloomfilter")
        bloomFilterDownloader = BloomFilterDownloader(
            URL("$serverUrl/checksum"),
            URL("$serverUrl/filter"),
            Uri.fromFile(file)
        )

        runBlocking {
            bloomFilterDownloader.download()
        }

        // Confirm that we hit the right endpoint.
        val checksumRequest = server.takeRequest()
        expectThat(checksumRequest.requestUrl?.toString()).isEqualTo("$serverUrl/checksum")
        val filterRequest = server.takeRequest()
        expectThat(filterRequest.requestUrl?.toString()).isEqualTo("$serverUrl/filter")

        expectThat(file.exists()).isFalse()
    }

    @Test
    fun enqueueDownloadTask_withValidUrl_enqueueSuccess() {
        val worker = TestListenableWorkerBuilder<BloomFilterDownloadWorker>(appContext).build()
        BloomFilterConfiguration.redditConfiguration.localUri = fileDir.toUri()

        runBlocking {
            val result = worker.doWork()
            expectThat(result).isEqualTo(ListenableWorker.Result.success())
        }
    }

    @Test
    fun enqueueDownloadTask_withInvalidUrl_assertionError() {
        val worker = TestListenableWorkerBuilder<BloomFilterDownloadWorker>(appContext).build()
        BloomFilterConfiguration.redditConfiguration.localUri = Uri.parse("www.example.com")

        assertThrows(AssertionError::class.java) {
            runBlocking {
                worker.doWork()
            }
        }
    }
}
