package io.github.conorryan.weeklies.data.import

import androidx.annotation.StringRes
import io.github.conorryan.weeklies.R

/** Error codes mirror the React Native app one-for-one. */
enum class RecipeImportErrorCode {
    CANCELED,
    EMPTY_CONTENT,
    INVALID_URL,
    NO_RECIPE_DATA,
    INCOMPLETE_RECIPE,
    PARSE_FAILED,
    FETCH_FAILED,
    PAGE_TOO_LARGE,
    MISTRAL_AUTH,
    MISTRAL_RATE_LIMIT,
    MISTRAL_UNAVAILABLE,
    MISTRAL_FAILED,
    MISTRAL_EMPTY,
    MISTRAL_PARSE,
    MISTRAL_KEY_MISSING,
    UNKNOWN,
}

/**
 * Copy shown for a failed import, as string resources so the UI resolves it
 * in the app's language. [messageArg] formats into [message].
 */
data class RecipeImportErrorDisplay(
    @StringRes val title: Int,
    @StringRes val message: Int,
    val messageArg: String? = null,
    @StringRes val hint: Int? = null,
)

class RecipeImportException(
    message: String,
    val code: RecipeImportErrorCode,
    /** Display copy for the message line; falls back to the code's default. */
    @StringRes val displayMessage: Int? = null,
    val displayArg: String? = null,
    @StringRes val displayHint: Int? = null,
) : Exception(message)

object RecipeImportErrors {

    private val TITLES: Map<RecipeImportErrorCode, Int> = mapOf(
        RecipeImportErrorCode.CANCELED to R.string.import_error_canceled,
        RecipeImportErrorCode.EMPTY_CONTENT to R.string.import_error_empty_content,
        RecipeImportErrorCode.INVALID_URL to R.string.import_error_invalid_url,
        RecipeImportErrorCode.NO_RECIPE_DATA to R.string.import_error_no_recipe_data,
        RecipeImportErrorCode.INCOMPLETE_RECIPE to R.string.import_error_incomplete_recipe,
        RecipeImportErrorCode.PARSE_FAILED to R.string.import_error_parse_failed,
        RecipeImportErrorCode.FETCH_FAILED to R.string.import_error_fetch_failed,
        RecipeImportErrorCode.PAGE_TOO_LARGE to R.string.import_error_page_too_large,
        RecipeImportErrorCode.MISTRAL_AUTH to R.string.import_error_mistral_auth,
        RecipeImportErrorCode.MISTRAL_RATE_LIMIT to R.string.import_error_mistral_rate_limit,
        RecipeImportErrorCode.MISTRAL_UNAVAILABLE to R.string.import_error_mistral_unavailable,
        RecipeImportErrorCode.MISTRAL_FAILED to R.string.import_error_mistral_failed,
        RecipeImportErrorCode.MISTRAL_EMPTY to R.string.import_error_mistral_failed,
        RecipeImportErrorCode.MISTRAL_PARSE to R.string.import_error_mistral_failed,
        RecipeImportErrorCode.MISTRAL_KEY_MISSING to R.string.import_error_mistral_key_missing,
        RecipeImportErrorCode.UNKNOWN to R.string.import_error_unknown,
    )

    fun canceled() = RecipeImportException(
        "Import canceled.",
        RecipeImportErrorCode.CANCELED,
        R.string.import_msg_canceled,
    )

    fun emptyContent() = RecipeImportException(
        "Paste or load page content before importing.",
        RecipeImportErrorCode.EMPTY_CONTENT,
        R.string.import_msg_empty_content,
    )

    fun missingUrl() = RecipeImportException(
        "Enter a recipe URL.",
        RecipeImportErrorCode.EMPTY_CONTENT,
        R.string.import_msg_missing_url,
    )

    fun invalidUrl() = RecipeImportException(
        "Enter a valid http or https URL.",
        RecipeImportErrorCode.INVALID_URL,
        R.string.import_msg_invalid_url,
    )

    fun noRecipeData() = RecipeImportException(
        "This page doesn't include recipe data we can read.",
        RecipeImportErrorCode.NO_RECIPE_DATA,
        R.string.import_msg_no_recipe_data,
        displayHint = R.string.import_hint_no_recipe_data,
    )

    fun incompleteRecipe() = RecipeImportException(
        "The page had recipe markup but no usable name and ingredients.",
        RecipeImportErrorCode.INCOMPLETE_RECIPE,
        R.string.import_msg_incomplete_recipe,
        displayHint = R.string.import_hint_incomplete_recipe,
    )

    fun parseFailed() = RecipeImportException(
        "Could not parse the page HTML.",
        RecipeImportErrorCode.PARSE_FAILED,
        R.string.import_msg_parse_failed,
        displayHint = R.string.import_hint_parse_failed,
    )

    fun fetchFailed(status: Int? = null) = RecipeImportException(
        if (status != null) "Could not load page (HTTP $status)." else "Could not load page.",
        RecipeImportErrorCode.FETCH_FAILED,
        if (status != null) R.string.import_msg_fetch_failed_status else R.string.import_msg_fetch_failed,
        displayArg = status?.toString(),
        displayHint = R.string.import_hint_fetch_failed,
    )

    fun pageTooLarge() = RecipeImportException(
        "Page is too large to import.",
        RecipeImportErrorCode.PAGE_TOO_LARGE,
        R.string.import_msg_page_too_large,
        displayHint = R.string.import_hint_page_too_large,
    )

    fun mistralKeyMissing() = RecipeImportException(
        "Add your Mistral API key in Settings → Recipe import.",
        RecipeImportErrorCode.MISTRAL_KEY_MISSING,
        R.string.import_msg_key_missing,
        displayHint = R.string.import_hint_key_missing,
    )

    fun mistralAuth() = RecipeImportException(
        "Invalid Mistral API key. Check Settings → Recipe import.",
        RecipeImportErrorCode.MISTRAL_AUTH,
        R.string.import_msg_mistral_auth,
    )

    fun mistralRateLimit() = RecipeImportException(
        "Mistral rate limit reached. Try again later.",
        RecipeImportErrorCode.MISTRAL_RATE_LIMIT,
        R.string.import_msg_mistral_rate_limit,
    )

    fun mistralUnavailable() = RecipeImportException(
        "Mistral is temporarily unavailable. Try again later.",
        RecipeImportErrorCode.MISTRAL_UNAVAILABLE,
        R.string.import_msg_mistral_unavailable,
    )

    fun mistralFailed(status: Int) =
        RecipeImportException(
            "Mistral request failed ($status).",
            RecipeImportErrorCode.MISTRAL_FAILED,
            R.string.import_msg_mistral_failed,
            displayArg = status.toString(),
        )

    fun mistralEmpty() = RecipeImportException(
        "Mistral returned an empty response.",
        RecipeImportErrorCode.MISTRAL_EMPTY,
        R.string.import_msg_mistral_empty,
        displayHint = R.string.import_hint_mistral_empty,
    )

    fun mistralParse() = RecipeImportException(
        "Could not parse recipe from Mistral response.",
        RecipeImportErrorCode.MISTRAL_PARSE,
        R.string.import_msg_mistral_parse,
        displayHint = R.string.import_hint_mistral_parse,
    )

    fun unknown() = RecipeImportException(
        "Something went wrong while importing.",
        RecipeImportErrorCode.UNKNOWN,
        R.string.import_msg_unknown,
        displayHint = R.string.import_hint_unknown,
    )

    fun isImportCanceled(error: Throwable?): Boolean {
        if (error is RecipeImportException && error.code == RecipeImportErrorCode.CANCELED) return true
        if (error is kotlinx.coroutines.CancellationException) return true
        val lower = (error?.message ?: "").lowercase()
        return lower.contains("cancel")
    }

    /** Maps any failure to display copy; null means "silently canceled". */
    fun format(error: Throwable?): RecipeImportErrorDisplay? {
        if (isImportCanceled(error)) return null

        if (error is RecipeImportException) {
            return RecipeImportErrorDisplay(
                title = TITLES.getValue(error.code),
                message = error.displayMessage ?: R.string.import_msg_unknown,
                messageArg = error.displayArg,
                hint = error.displayHint,
            )
        }

        return RecipeImportErrorDisplay(
            title = TITLES.getValue(RecipeImportErrorCode.UNKNOWN),
            message = R.string.import_msg_unknown,
            hint = R.string.import_hint_unknown,
        )
    }
}
