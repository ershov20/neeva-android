// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

enum class LoadingState {
    UNINITIALIZED, LOADING, READY;

    companion object {
        fun from(vararg states: LoadingState): LoadingState {
            return when {
                states.all { it == READY } -> READY
                states.any { it == LOADING } -> LOADING
                else -> UNINITIALIZED
            }
        }
    }
}
