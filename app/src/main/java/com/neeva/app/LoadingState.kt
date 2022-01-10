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
