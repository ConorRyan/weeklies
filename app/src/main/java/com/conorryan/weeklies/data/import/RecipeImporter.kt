package com.conorryan.weeklies.data.import

import com.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Orchestrators for recipe import: from pasted HTML or from a URL, via the
 * JSON-LD scraper or via Mistral. Cancellation is handled through structured
 * concurrency (the caller's coroutine being canceled aborts everything).
 */
class RecipeImporter(
    private val pageFetcher: PageFetcher,
    private val mistralClient: MistralClient,
) {

    /** Standard import from pasted HTML. */
    suspend fun importRecipeFromHtmlWithScraper(html: String, sourceUrl: String? = null): NewRecipe {
        val trimmed = html.trim()
        if (trimmed.isEmpty()) throw RecipeImportErrors.emptyContent()

        coroutineContext.ensureActive()

        val scraped = parseRecipeHtml(trimmed, sourceUrl?.trim())

        coroutineContext.ensureActive()

        return scrapedRecipeToNewRecipe(scraped) ?: throw RecipeImportErrors.incompleteRecipe()
    }

    /** Standard import from a URL: fetches the page first. */
    suspend fun importRecipeFromUrlWithScraper(url: String): NewRecipe {
        val html = pageFetcher.fetchRecipePageHtml(url)
        return importRecipeFromHtmlWithScraper(html, sourceUrl = url.trim())
    }

    /** Import with AI from pasted HTML. */
    suspend fun importRecipeWithMistral(html: String, apiKey: String, sourceUrl: String? = null): NewRecipe {
        val trimmed = html.trim()
        if (trimmed.isEmpty()) throw RecipeImportErrors.emptyContent()
        val preprocessed = preprocessRecipeHtml(trimmed)
        return mistralClient.extractRecipe(apiKey, preprocessed, sourceUrl)
    }

    /** Import with AI from a URL: fetches the page first. */
    suspend fun importRecipeFromUrlWithMistral(url: String, apiKey: String): NewRecipe {
        val html = pageFetcher.fetchRecipePageHtml(url)
        return importRecipeWithMistral(html, apiKey, sourceUrl = url.trim())
    }
}
