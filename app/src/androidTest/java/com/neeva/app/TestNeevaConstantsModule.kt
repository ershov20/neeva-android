package com.neeva.app

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NeevaConstantsModule::class]
)
object TestNeevaConstantsModule {
    /**
     * Sends the user to localhost instead of out to the real Neeva site.  Cookies are still set on
     * neeva.com to avoid WebLayer complaining about setting a cookie on localhost.
     */
    val neevaConstants = object : NeevaConstants(
        appHost = "127.0.0.1:8000",
        appURL = "http://127.0.0.1:8000/",
        cookieHost = "neeva.com",
        cookieURL = "https://neeva.com"
    ) {
        override val appHelpCenterURL = "http://127.0.0.1:8000/help.html"
        override val homepageURL = "http://127.0.0.1:8000"
    }

    @Provides
    @Singleton
    fun providesNeevaConstants(): NeevaConstants {
        return neevaConstants
    }
}
