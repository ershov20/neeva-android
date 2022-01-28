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
