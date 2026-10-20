package com.conorryan.weeklies.ui

import kotlinx.coroutines.flow.MutableStateFlow

/** Message shown by the root snackbar host; replaces the RN toast system. */
data class ToastEvent(val title: String, val message: String? = null, val isError: Boolean = false)

class ToastBus {
    val current = MutableStateFlow<ToastEvent?>(null)

    fun info(title: String, message: String? = null) {
        current.value = ToastEvent(title = title, message = message, isError = false)
    }

    fun error(title: String, message: String? = null) {
        current.value = ToastEvent(title = title, message = message, isError = true)
    }

    fun clear() {
        current.value = null
    }
}
