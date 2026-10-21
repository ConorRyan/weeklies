package io.github.conorryan.weeklies.data.import

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

data class RecipeImportErrorDisplay(val title: String, val message: String, val hint: String? = null)

class RecipeImportException(
    message: String,
    val code: RecipeImportErrorCode,
    val hint: String? = null,
) : Exception(message)

object RecipeImportErrors {

    private val TITLES: Map<RecipeImportErrorCode, String> = mapOf(
        RecipeImportErrorCode.CANCELED to "Import canceled",
        RecipeImportErrorCode.EMPTY_CONTENT to "Nothing to import",
        RecipeImportErrorCode.INVALID_URL to "Invalid URL",
        RecipeImportErrorCode.NO_RECIPE_DATA to "No recipe found",
        RecipeImportErrorCode.INCOMPLETE_RECIPE to "Incomplete recipe",
        RecipeImportErrorCode.PARSE_FAILED to "Could not read page",
        RecipeImportErrorCode.FETCH_FAILED to "Could not load page",
        RecipeImportErrorCode.PAGE_TOO_LARGE to "Page too large",
        RecipeImportErrorCode.MISTRAL_AUTH to "Mistral API key",
        RecipeImportErrorCode.MISTRAL_RATE_LIMIT to "Rate limit",
        RecipeImportErrorCode.MISTRAL_UNAVAILABLE to "Mistral unavailable",
        RecipeImportErrorCode.MISTRAL_FAILED to "Import with AI failed",
        RecipeImportErrorCode.MISTRAL_EMPTY to "Import with AI failed",
        RecipeImportErrorCode.MISTRAL_PARSE to "Import with AI failed",
        RecipeImportErrorCode.MISTRAL_KEY_MISSING to "Mistral API key",
        RecipeImportErrorCode.UNKNOWN to "Import failed",
    )

    fun canceled() = RecipeImportException("Import canceled.", RecipeImportErrorCode.CANCELED)

    fun emptyContent() = RecipeImportException(
        "Paste or load page content before importing.",
        RecipeImportErrorCode.EMPTY_CONTENT,
    )

    fun missingUrl() = RecipeImportException(
        "Enter a recipe URL.",
        RecipeImportErrorCode.EMPTY_CONTENT,
    )

    fun invalidUrl() = RecipeImportException(
        "Enter a valid http or https URL.",
        RecipeImportErrorCode.INVALID_URL,
    )

    fun noRecipeData() = RecipeImportException(
        "This page doesn't include recipe data we can read.",
        RecipeImportErrorCode.NO_RECIPE_DATA,
        "Try a different recipe page, paste the full page HTML on web, or use Import with AI.",
    )

    fun incompleteRecipe() = RecipeImportException(
        "The page had recipe markup but no usable name and ingredients.",
        RecipeImportErrorCode.INCOMPLETE_RECIPE,
        "Try Import with AI or create the recipe manually.",
    )

    fun parseFailed() = RecipeImportException(
        "Could not parse the page HTML.",
        RecipeImportErrorCode.PARSE_FAILED,
        "Make sure you copied the full page source, not just the visible text.",
    )

    fun fetchFailed(status: Int? = null) = RecipeImportException(
        if (status != null) "Could not load page (HTTP $status)." else "Could not load page.",
        RecipeImportErrorCode.FETCH_FAILED,
        "Check the URL, your connection, and that the site allows fetching this page.",
    )

    fun pageTooLarge() = RecipeImportException(
        "Page is too large to import.",
        RecipeImportErrorCode.PAGE_TOO_LARGE,
        "Try a shorter recipe page or paste the HTML on web.",
    )

    fun mistralKeyMissing() = RecipeImportException(
        "Add your Mistral API key in Settings → Recipe import.",
        RecipeImportErrorCode.MISTRAL_KEY_MISSING,
        "Import with AI needs a key; standard Import does not.",
    )

    fun mistralAuth() = RecipeImportException(
        "Invalid Mistral API key. Check Settings → Recipe import.",
        RecipeImportErrorCode.MISTRAL_AUTH,
    )

    fun mistralRateLimit() = RecipeImportException(
        "Mistral rate limit reached. Try again later.",
        RecipeImportErrorCode.MISTRAL_RATE_LIMIT,
    )

    fun mistralUnavailable() = RecipeImportException(
        "Mistral is temporarily unavailable. Try again later.",
        RecipeImportErrorCode.MISTRAL_UNAVAILABLE,
    )

    fun mistralFailed(status: Int) =
        RecipeImportException("Mistral request failed ($status).", RecipeImportErrorCode.MISTRAL_FAILED)

    fun mistralEmpty() = RecipeImportException(
        "Mistral returned an empty response.",
        RecipeImportErrorCode.MISTRAL_EMPTY,
        "Try again, or use standard Import if the page has JSON-LD recipe data.",
    )

    fun mistralParse() = RecipeImportException(
        "Could not parse recipe from Mistral response.",
        RecipeImportErrorCode.MISTRAL_PARSE,
        "Try again, or use standard Import if the page has JSON-LD recipe data.",
    )

    fun unknown() = RecipeImportException(
        "Something went wrong while importing.",
        RecipeImportErrorCode.UNKNOWN,
        "Try again, or use Import with AI if standard Import cannot read the page.",
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
                message = error.message ?: "",
                hint = error.hint,
            )
        }

        val message = error?.message
        return RecipeImportErrorDisplay(
            title = TITLES.getValue(RecipeImportErrorCode.UNKNOWN),
            message = if (message.isNullOrBlank()) {
                unknown().message ?: "Something went wrong while importing."
            } else {
                message
            },
            hint = unknown().hint,
        )
    }
}

object ImportConstants {
    const val MISTRAL_CHAT_URL = "https://api.mistral.ai/v1/chat/completions"
    const val MISTRAL_RECIPE_MODEL = "mistral-small-latest"

    /** One-off cleanup gets the more careful model; import stays on small. */
    const val MISTRAL_CLEANUP_MODEL = "mistral-medium-latest"

    /** Stored recipes are always normalized to this serving count. */
    const val RECIPE_IMPORT_TARGET_SERVINGS = 1

    /** Max characters sent to Mistral after preprocessing. */
    const val MAX_HTML_FOR_LLM = 100_000

    const val NATIVE_FETCH_TIMEOUT_MS = 15_000L
    const val NATIVE_MAX_RESPONSE_BYTES = 5L * 1024 * 1024
}
