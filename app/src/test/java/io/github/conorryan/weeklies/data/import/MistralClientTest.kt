package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Fake transport returning queued responses, recording requests. */
private class FakeTransport(vararg responses: HttpResponse) : HttpTransport {
    val requests = mutableListOf<Pair<String, String>>()
    private val queue = responses.toMutableList()

    override suspend fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): HttpResponse {
        requests.add(method to (body ?: ""))
        check(queue.isNotEmpty()) { "no queued response" }
        return queue.removeAt(0)
    }
}

private fun chatResponse(content: String): HttpResponse = HttpResponse(
    status = 200,
    bodyText = """{"choices":[{"message":{"content":${kotlinx.serialization.json.JsonPrimitive(content)}}}]}""",
)

class MistralClientTest {

    private fun preprocessed(): PreprocessedRecipePage =
        PreprocessedRecipePage(jsonLdRecipes = emptyList(), htmlSnippet = "Pasta content")

    @Test
    fun extractsRecipeFromValidResponse() = runTest {
        val content = """{"found":true,"name":"Pasta","servings":4,"ingredients":[{"name":"pasta","quantity":"400 g"}]}"""
        val transport = FakeTransport(chatResponse(content))
        val recipe = MistralClient(transport).extractRecipe("key", preprocessed())

        assertEquals(NewRecipe(name = "Pasta", ingredients = listOf("100 g pasta")), recipe)
    }

    @Test
    fun throwsNoRecipeWhenFoundIsFalse() = runTest {
        val content = """{"found":false,"name":"","servings":1,"ingredients":[]}"""
        val transport = FakeTransport(chatResponse(content))
        try {
            MistralClient(transport).extractRecipe("key", preprocessed())
            fail("expected noRecipeData")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.NO_RECIPE_DATA, e.code)
        }
    }

    @Test
    fun retriesOnceWhenResponseIsNotValidJson() = runTest {
        val transport = FakeTransport(
            chatResponse("this is not json"),
            chatResponse("""{"found":true,"name":"Soup","servings":2,"ingredients":[{"name":"water"}]}"""),
        )
        val recipe = MistralClient(transport).extractRecipe("key", preprocessed())

        // No numeric quantity on the line, so scaling leaves it untouched.
        assertEquals(NewRecipe(name = "Soup", ingredients = listOf("water")), recipe)
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun givesUpAfterRetryStillFails() = runTest {
        val transport = FakeTransport(
            chatResponse("bad"),
            chatResponse("still bad"),
        )
        try {
            MistralClient(transport).extractRecipe("key", preprocessed())
            fail("expected mistralParse")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_PARSE, e.code)
        }
    }

    @Test
    fun mapsHttpErrorsToRecipeImportErrors() = runTest {
        for ((status, code) in listOf(
            401 to RecipeImportErrorCode.MISTRAL_AUTH,
            429 to RecipeImportErrorCode.MISTRAL_RATE_LIMIT,
            503 to RecipeImportErrorCode.MISTRAL_UNAVAILABLE,
            404 to RecipeImportErrorCode.MISTRAL_FAILED,
        )) {
            val transport = FakeTransport(HttpResponse(status, "{}"))
            try {
                MistralClient(transport).extractRecipe("key", preprocessed())
                fail("expected error for status $status")
            } catch (e: RecipeImportException) {
                assertEquals(code, e.code)
            }
        }
    }

    @Test
    fun throwsKeyMissingForBlankApiKey() = runTest {
        val transport = FakeTransport()
        try {
            MistralClient(transport).extractRecipe("  ", preprocessed())
            fail("expected key missing")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_KEY_MISSING, e.code)
        }
        assertEquals(0, transport.requests.size)
    }

    @Test
    fun throwsEmptyWhenContentIsBlank() = runTest {
        val transport = FakeTransport(chatResponse("  "))
        try {
            MistralClient(transport).extractRecipe("key", preprocessed())
            fail("expected mistralEmpty")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_EMPTY, e.code)
        }
    }

    @Test
    fun requestIncludesSchemaAndSystemPrompt() = runTest {
        val content = """{"found":true,"name":"A","servings":1,"ingredients":[{"name":"x"}]}"""
        val transport = FakeTransport(chatResponse(content))
        MistralClient(transport).extractRecipe("key", preprocessed(), sourceUrl = "https://example.com")

        val body = transport.requests.single().second
        assertTrue(body.contains("\"json_schema\""))
        assertTrue(body.contains("recipe_extract"))
        assertTrue(body.contains("You extract recipe data from web page content"))
        assertTrue(body.contains("Source URL: https://example.com"))
    }
}

class PageFetcherTest {

    @Test
    fun rejectsInvalidUrls() = runTest {
        val fetcher = PageFetcher(FakeTransport(HttpResponse(200, "<html/>")))
        try {
            fetcher.fetchRecipePageHtml("ftp://example.com")
            fail("expected invalidUrl")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.INVALID_URL, e.code)
        }
    }

    @Test
    fun throwsFetchFailedOnHttpError() = runTest {
        val fetcher = PageFetcher(FakeTransport(HttpResponse(404, "")))
        try {
            fetcher.fetchRecipePageHtml("https://example.com")
            fail("expected fetchFailed")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.FETCH_FAILED, e.code)
        }
    }

    @Test
    fun throwsPageTooLargeOnHugeBody() = runTest {
        val huge = "x".repeat(ImportConstants.NATIVE_MAX_RESPONSE_BYTES.toInt() + 1)
        val fetcher = PageFetcher(FakeTransport(HttpResponse(200, huge)))
        try {
            fetcher.fetchRecipePageHtml("https://example.com")
            fail("expected pageTooLarge")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.PAGE_TOO_LARGE, e.code)
        }
    }

    @Test
    fun returnsBodyOnSuccess() = runTest {
        val fetcher = PageFetcher(FakeTransport(HttpResponse(200, "<html><p>ok</p></html>")))
        assertEquals("<html><p>ok</p></html>", fetcher.fetchRecipePageHtml("https://example.com"))
    }
}

class RecipeImporterTest {

    private fun fixture(name: String): String =
        object {}.javaClass.getResource("/recipe-import/$name")!!.readText()

    private fun importer(transport: HttpTransport) =
        RecipeImporter(PageFetcher(transport), MistralClient(transport))

    @Test
    fun importsRecipeFromHtmlFixture() = runTest {
        val recipe = Scraper.extractRecipe(fixture("allrecipes-with-jsonld.html"))
        assertEquals("Tomato Pasta", recipe.name)
        // recipeYield "2 servings" scaled to one portion: 400g -> 200 g.
        assertEquals(listOf("200 g pasta", "1 tomatoes"), recipe.ingredients)
    }

    @Test
    fun throwsEmptyContentForBlankHtml() = runTest {
        try {
            Scraper.extractRecipe("   ")
            fail("expected emptyContent")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.EMPTY_CONTENT, e.code)
        }
    }

    @Test
    fun throwsIncompleteRecipeWhenYieldMarkupIsUnusable() = runTest {
        val html = """<script type="application/ld+json">{"@type":"Recipe","name":"No ingredients"}</script>"""
        try {
            Scraper.extractRecipe(html)
            fail("expected noRecipeData")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.NO_RECIPE_DATA, e.code)
        }
    }

    @Test
    fun importsFromUrlByFetchingFirst() = runTest {
        val transport = FakeTransport(HttpResponse(200, fixture("allrecipes-with-jsonld.html")))
        val recipe = importer(transport)
            .importRecipeFromUrlWithScraper("https://example.com/pasta")
        assertEquals("Tomato Pasta", recipe.name)
        assertEquals(1, transport.requests.size)
    }
}
