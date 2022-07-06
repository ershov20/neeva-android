package com.neeva.app.publicsuffixlist

import android.net.Uri

/**
 * Derives the registerable domain name from the provided domain.
 * Returns the base domain of a particular [Uri], or null if it couldn't be parsed.
 */
fun interface DomainProvider {
    fun getRegisteredDomain(uri: Uri?): String?
}

val previewDomainProvider: DomainProvider by lazy {
    return@lazy DomainProvider { uri ->
        uri?.authority?.split(".")?.takeLast(2)?.joinToString(".")
    }
}
