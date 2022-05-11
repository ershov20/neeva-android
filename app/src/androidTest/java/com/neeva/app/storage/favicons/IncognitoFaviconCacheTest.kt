package com.neeva.app.storage.favicons

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.Dispatchers
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.storage.toByteArray
import com.neeva.app.storage.toLetterBitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class IncognitoFaviconCacheTest {
    private lateinit var context: Context
    private lateinit var domainProvider: DomainProvider
    private lateinit var dispatchers: Dispatchers
    private lateinit var incognitoFaviconCache: IncognitoFaviconCache

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val filesDir = context.cacheDir

        runBlocking {
            domainProvider = DomainProviderImpl(context).apply {
                initialize()
            }
        }

        dispatchers = Dispatchers(
            kotlinx.coroutines.Dispatchers.Main,
            kotlinx.coroutines.Dispatchers.Main
        )

        incognitoFaviconCache = IncognitoFaviconCache(
            appContext = context,
            filesDir = filesDir,
            domainProvider = domainProvider,
            dispatchers = dispatchers
        )
    }

    @Test
    fun saveAndLoad() = runTest {
        val uri = Uri.parse("http://example.com")
        val faviconBitmap = "D".toLetterBitmap(0.50f, Color.BLACK)

        val favicon = incognitoFaviconCache.saveFavicon(uri, faviconBitmap)
        val loadedBitmap = incognitoFaviconCache.loadFavicon(Uri.parse(favicon!!.faviconURL!!))
        expectThat(loadedBitmap).isNotNull()

        val originalBytes = faviconBitmap.toByteArray()
        val loadedBytes = loadedBitmap!!.toByteArray()
        expectThat(loadedBytes).isEqualTo(originalBytes)
    }
}
