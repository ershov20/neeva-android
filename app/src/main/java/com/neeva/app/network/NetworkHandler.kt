// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED

/** Checks to see if the device has internet access. */
open class NetworkHandler(appContext: Context) {
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)

    open fun isConnectedToInternet(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork
        if (connectivityManager == null || activeNetwork == null) {
            return false
        }
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NET_CAPABILITY_VALIDATED) ?: false
    }
}
