package io.github.conorryan.weeklies.ui

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Message shown by the root snackbar host; replaces the RN toast system.
 * Carries string resources (plus optional format args) so the snackbar
 * resolves in the app's language at display time.
 */
data class ToastEvent(
    @StringRes val title: Int,
    val titleArg: String? = null,
    @StringRes val message: Int? = null,
    val messageArg: String? = null,
    val isError: Boolean = false,
)

class ToastBus {
    val current = MutableStateFlow<ToastEvent?>(null)

    fun info(@StringRes title: Int, titleArg: String? = null, @StringRes message: Int? = null) {
        current.value = ToastEvent(title = title, titleArg = titleArg, message = message, isError = false)
    }

    fun error(
        @StringRes title: Int,
        @StringRes message: Int? = null,
        titleArg: String? = null,
        messageArg: String? = null,
    ) {
        current.value = ToastEvent(
            title = title,
            titleArg = titleArg,
            message = message,
            messageArg = messageArg,
            isError = true,
        )
    }

    fun clear() {
        current.value = null
    }
}
