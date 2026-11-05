package io.github.conorryan.weeklies.data.import

/** Tuning knobs and endpoints of the recipe import pipeline. */
object ImportConstants {
    const val MISTRAL_CHAT_URL = "https://api.mistral.ai/v1/chat/completions"

    /** The Mistral model every AI feature uses: import, cleanup, calorie estimates. */
    const val MISTRAL_MODEL = "mistral-large-latest"

    /** Stored recipes are always normalized to this serving count. */
    const val RECIPE_IMPORT_TARGET_SERVINGS = 1

    /** Max characters sent to Mistral after preprocessing. */
    const val MAX_HTML_FOR_LLM = 100_000

    const val NATIVE_FETCH_TIMEOUT_MS = 15_000L
    const val NATIVE_MAX_RESPONSE_BYTES = 5L * 1024 * 1024
}
