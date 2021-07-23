package com.neeva.app.web

import android.net.Uri

fun Uri.baseDomain() : String? {
    val authority = this.authority ?: return null
    val authoritySplit = authority.split(".")
    if (authoritySplit.size < 2) return null
    return authoritySplit.takeLast(2).joinToString(".")
}