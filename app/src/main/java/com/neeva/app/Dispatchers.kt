// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import kotlinx.coroutines.CoroutineDispatcher

/** Dispatchers that can be used to send execution to different threads. */
data class Dispatchers(
    val main: CoroutineDispatcher,
    val io: CoroutineDispatcher
)

val previewDispatchers by lazy {
    Dispatchers(kotlinx.coroutines.Dispatchers.Main.immediate, kotlinx.coroutines.Dispatchers.IO)
}
