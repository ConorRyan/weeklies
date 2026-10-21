package com.conorryan.weeklies.data.import

import com.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Orchestrators for recipe import from a URL, via the JSON-LD scraper or via
 * Mistral. Cancellation is handled through structured concurrency (the
 * caller's coroutine being canceled aborts everything).
 */
class RecipeImporter(
    private val pageFetcher: PageFetcher,
    private val mistralClient: MistralClient,
) {

    /** Standard import from a URL: fetches the page, then scrapes it. */
    suspend fun importRecipeFromUrlWithScraper(url: String): NewRecipe {
        val html = pageFetcher.fetchRecipePageHtml(url)
        coroutineContext.ensureActive()
        return Scraper.extractRecipe(html, url.trim())
    }

    /** Import with AI from a URL: fetches the page, then asks Mistral. */
    suspend fun importRecipeFromUrlWithMistral(url: String, apiKey: String): NewRecipe {
        val html = pageFetcher.fetchRecipePageHtml(url)
        coroutineContext.ensureActive()
        val preprocessed = preprocessRecipeHtml(html)
        return mistralClient.extractRecipe(apiKey, preprocessed, url.trim())
    }
}
