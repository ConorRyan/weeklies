package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.model.NewRecipe

/**
 * JSON-LD scraper: a raw recipe page in, a one-portion recipe out. Owns the
 * parse-then-convert ordering so callers cannot get it wrong; the step
 * functions it composes are internal seams exercised by ScraperTest.
 */
object Scraper {

    /** Extracts and scales a recipe from page HTML. */
    fun extractRecipe(html: String, url: String? = null): NewRecipe {
        val trimmed = html.trim()
        if (trimmed.isEmpty()) throw RecipeImportErrors.emptyContent()
        val scraped = parseRecipeHtml(trimmed, url?.trim())
        return scrapedRecipeToNewRecipe(scraped) ?: throw RecipeImportErrors.incompleteRecipe()
    }
}
