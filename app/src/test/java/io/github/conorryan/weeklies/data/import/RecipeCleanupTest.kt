package io.github.conorryan.weeklies.data.import

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Fake transport returning queued responses, recording requests. */
private class CleanupFakeTransport(vararg responses: HttpResponse) : HttpTransport {
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

private fun cleanupChatResponse(content: String): HttpResponse = HttpResponse(
    status = 200,
    bodyText = """{"choices":[{"message":{"content":${JsonPrimitive(content)}}}]}""",
)

class ParseCleanupResponseTest {

    @Test
    fun parsesIngredientLinesAndDeclaredOriginsFromValidContent() {
        val content = """{"ingredients":[{"name":"flour","quantity":"250 g","from":[0]},{"name":"salt","quantity":"1 tsp","from":[1]}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("250 g flour", "1 tsp salt"), from = listOf(listOf(0), listOf(1))),
            parseCleanupResponse(content),
        )
    }

    @Test
    fun parsesMergedOrigins() {
        val content = """{"ingredients":[{"name":"pasta","quantity":"250 g","from":[0,1]}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("250 g pasta"), from = listOf(listOf(0, 1))),
            parseCleanupResponse(content),
        )
    }

    @Test
    fun allowsMissingQuantityForToTasteItems() {
        val content = """{"ingredients":[{"name":"pepper","quantity":"","from":[0]}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("pepper"), from = listOf(listOf(0))),
            parseCleanupResponse(content),
        )
    }

    @Test
    fun toleratesGarbageOrMissingOrigins() {
        val garbage = """{"ingredients":[{"name":"flour","quantity":"250 g","from":["a",99]}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("250 g flour"), from = listOf(listOf(99))),
            parseCleanupResponse(garbage),
        )
        val missing = """{"ingredients":[{"name":"flour","quantity":"250 g"}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("250 g flour"), from = listOf(emptyList())),
            parseCleanupResponse(missing),
        )
    }

    @Test
    fun returnsNullForInvalidJson() {
        assertEquals(null, parseCleanupResponse("not json"))
    }

    @Test
    fun returnsNullWhenIngredientsAreMissingOrEmpty() {
        assertEquals(null, parseCleanupResponse("""{"name":"nope"}"""))
        assertEquals(null, parseCleanupResponse("""{"ingredients":[]}"""))
    }

    @Test
    fun stripsUnitRepeatedInIngredientName() {
        val content = """{"ingredients":[{"name":"garlic cloves","quantity":"1 clove","from":[0]},{"name":"bread slices","quantity":"2 slices","from":[1]}]}"""
        assertEquals(
            CleanedIngredients(lines = listOf("1 clove garlic", "2 slices bread"), from = listOf(listOf(0), listOf(1))),
            parseCleanupResponse(content),
        )
    }
}

class IsValidCleanupMappingTest {

    @Test
    fun acceptsFullCoverageMappings() {
        assertTrue(isValidCleanupMapping(listOf(listOf(0), listOf(1), listOf(2)), originalCount = 3))
        assertTrue(isValidCleanupMapping(listOf(listOf(0, 1), listOf(2)), originalCount = 3))
    }

    @Test
    fun acceptsDeclaredSplits() {
        assertTrue(isValidCleanupMapping(listOf(listOf(0), listOf(0)), originalCount = 1))
        assertTrue(isValidCleanupMapping(listOf(listOf(0), listOf(0), listOf(1)), originalCount = 2))
    }

    @Test
    fun rejectsBrokenMappings() {
        assertEquals(false, isValidCleanupMapping(listOf(listOf(0), listOf(5)), originalCount = 2))
        assertEquals(false, isValidCleanupMapping(listOf(listOf(0)), originalCount = 2))
        assertEquals(false, isValidCleanupMapping(listOf(emptyList(), listOf(0)), originalCount = 1))
        assertEquals(false, isValidCleanupMapping(listOf(listOf(0, 0)), originalCount = 1))
        assertEquals(false, isValidCleanupMapping(emptyList(), originalCount = 1))
    }
}

class PairCleanupForReviewTest {

    @Test
    fun pairsExactlyFromAValidDeclaredMapping() {
        val before = listOf("100 g pasta", "150 g pasta", "1 onion")
        val cleaned = CleanedIngredients(
            lines = listOf("250 g pasta", "1 onion"),
            from = listOf(listOf(0, 1), listOf(2)),
        )
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("100 g pasta", "150 g pasta"), after = listOf("250 g pasta")),
                CleanupPairing(before = listOf("1 onion"), after = listOf("1 onion")),
            ),
            pairCleanupForReview(before, cleaned),
        )
    }

    @Test
    fun declaredMappingPairsRenamesThatNameMatchingCannot() {
        val before = listOf("1 green onion")
        val cleaned = CleanedIngredients(lines = listOf("1 scallion"), from = listOf(listOf(0)))
        assertEquals(
            listOf(CleanupPairing(before = listOf("1 green onion"), after = listOf("1 scallion"))),
            pairCleanupForReview(before, cleaned),
        )
    }

    @Test
    fun invalidMappingFallsBackToNameBasedAlignment() {
        val before = listOf("100 g pasta")
        val cleaned = CleanedIngredients(lines = listOf("250 g pasta"), from = listOf(listOf(3)))
        assertEquals(
            listOf(CleanupPairing(before = listOf("100 g pasta"), after = listOf("250 g pasta"))),
            pairCleanupForReview(before, cleaned),
        )
    }

    @Test
    fun declaredSplitGroupsIntoOneRow() {
        val before = listOf("salt and pepper, to taste")
        val cleaned = CleanedIngredients(
            lines = listOf("salt", "pepper"),
            from = listOf(listOf(0), listOf(0)),
        )
        assertEquals(
            listOf(
                CleanupPairing(
                    before = listOf("salt and pepper, to taste"),
                    after = listOf("salt", "pepper"),
                ),
            ),
            pairCleanupForReview(before, cleaned),
        )
    }

    @Test
    fun declaredRowsKeepOriginalOrder() {
        val before = listOf("1 onion", "100 g pasta", "150 g pasta")
        val cleaned = CleanedIngredients(
            lines = listOf("250 g pasta", "1 onion"),
            from = listOf(listOf(1, 2), listOf(0)),
        )
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("1 onion"), after = listOf("1 onion")),
                CleanupPairing(before = listOf("100 g pasta", "150 g pasta"), after = listOf("250 g pasta")),
            ),
            pairCleanupForReview(before, cleaned),
        )
    }
}

class RemoveDuplicatedUnitTest {

    @Test
    fun stripsSingularAndPluralUnitsFromNameEnd() {
        assertEquals("garlic", removeDuplicatedUnit("1 clove", "garlic cloves"))
        assertEquals("bread", removeDuplicatedUnit("2 slices", "bread slices"))
        assertEquals("garlic", removeDuplicatedUnit("1 clove", "garlic clove"))
    }

    @Test
    fun stripsIrregularPluralUnits() {
        assertEquals("bay", removeDuplicatedUnit("1 leaf", "bay leaves"))
        assertEquals("bay", removeDuplicatedUnit("1 leaf", "bay leaf"))
    }

    @Test
    fun leavesNamesWithoutRepeatedUnitsAlone() {
        assertEquals("flour", removeDuplicatedUnit("250 g", "flour"))
        assertEquals("olive oil", removeDuplicatedUnit("2 tbsp", "olive oil"))
        assertEquals("green onion", removeDuplicatedUnit("1", "green onion"))
    }

    @Test
    fun keepsTheNameWhenStrippingWouldEmptyIt() {
        assertEquals("cloves", removeDuplicatedUnit("1 clove", "cloves"))
        assertEquals("leaves", removeDuplicatedUnit("1 leaf", "leaves"))
    }
}

class PairCleanupIngredientsTest {

    @Test
    fun pairsLinesOneToOneWhenCountsMatch() {
        val before = listOf("2 cups flour", "1 cup milk")
        val after = listOf("250 g flour", "240 ml milk")
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("2 cups flour"), after = listOf("250 g flour")),
                CleanupPairing(before = listOf("1 cup milk"), after = listOf("240 ml milk")),
            ),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun matchesIngredientsAcrossUnitsAndCase() {
        val before = listOf("2 Cups Flour!")
        val after = listOf("250 g flour")
        assertEquals(
            listOf(CleanupPairing(before = before, after = after)),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun groupsMergedDuplicatesIntoOnePairing() {
        val before = listOf("100 g pasta", "150 g pasta")
        val after = listOf("250 g pasta")
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("100 g pasta", "150 g pasta"), after = listOf("250 g pasta")),
            ),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun alignsAMergeInTheMiddleWithoutMispairingTheRest() {
        val before = listOf("200 g flour", "100 g pasta", "150 g pasta", "1 onion")
        val after = listOf("200 g flour", "250 g pasta", "1 onion")
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("200 g flour"), after = listOf("200 g flour")),
                CleanupPairing(before = listOf("100 g pasta", "150 g pasta"), after = listOf("250 g pasta")),
                CleanupPairing(before = listOf("1 onion"), after = listOf("1 onion")),
            ),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun groupsSplitsIntoOnePairing() {
        val before = listOf("1 cup milk")
        val after = listOf("120 ml milk", "120 ml milk")
        assertEquals(
            listOf(CleanupPairing(before = listOf("1 cup milk"), after = listOf("120 ml milk", "120 ml milk"))),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun unrelatedLinesRenderAsDroppedAndAddedNotAsAWrongPair() {
        val before = listOf("2 eggs")
        val after = listOf("1 lemon")
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("2 eggs"), after = emptyList()),
                CleanupPairing(before = emptyList(), after = listOf("1 lemon")),
            ),
            pairCleanupIngredients(before, after),
        )
    }

    @Test
    fun reorderingNeverManufacturesSplitRows() {
        // Regression: the model nudged the egg line below spaghetti, which the
        // greedy alignment once displayed as a fake split (egg -> spaghetti +
        // egg) plus a cascade of mispaired lines.
        val before = listOf("1 large egg", "100 g spaghetti", "50 g unsalted butter")
        val after = listOf("100 g spaghetti", "1 egg (large)", "50 g unsalted butter")
        val pairings = pairCleanupIngredients(before, after)

        // Explicit expectation: the moved egg renders as dropped + added,
        // and matching lines pair correctly — never a fake split.
        assertEquals(
            listOf(
                CleanupPairing(before = listOf("1 large egg"), after = emptyList()),
                CleanupPairing(before = listOf("100 g spaghetti"), after = listOf("100 g spaghetti")),
                CleanupPairing(before = emptyList(), after = listOf("1 egg (large)")),
                CleanupPairing(before = listOf("50 g unsalted butter"), after = listOf("50 g unsalted butter")),
            ),
            pairings,
        )
    }
}

class IsUnchangedPairingTest {

    @Test
    fun trueOnlyForIdenticalOneToOnePairings() {
        assertTrue(isUnchangedPairing(CleanupPairing(listOf("250 g flour"), listOf("250 g flour"))))
        assertEquals(false, isUnchangedPairing(CleanupPairing(listOf("2 cups flour"), listOf("250 g flour"))))
        assertEquals(false, isUnchangedPairing(CleanupPairing(listOf("1 cup flour"), listOf("1 cup flour", "1 cup flour"))))
    }

    @Test
    fun falseForMergesEvenWhenTheResultMatchesAnOriginal() {
        assertEquals(
            false,
            isUnchangedPairing(
                CleanupPairing(
                    listOf("100 g pasta", "100 g pasta"),
                    listOf("100 g pasta"),
                ),
            ),
        )
    }
}

class RecipeCleanerTest {

    @Test
    fun cleansIngredientLinesFromValidResponse() = runTest {
        val content = """{"ingredients":[{"name":"flour","quantity":"250 g","from":[0]},{"name":"milk","quantity":"240 ml","from":[1]}]}"""
        val transport = CleanupFakeTransport(cleanupChatResponse(content))
        val result = RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("2 cups flour", "1 cup milk"))

        assertEquals(listOf("250 g flour", "240 ml milk"), result.lines)
        assertEquals(listOf(listOf(0), listOf(1)), result.from)
    }

    @Test
    fun sendsRecipeNameAndLinesToMistral() = runTest {
        val content = """{"ingredients":[{"name":"flour","quantity":"250 g"}]}"""
        val transport = CleanupFakeTransport(cleanupChatResponse(content))
        RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("2.1 cups flour"))

        val body = transport.requests.single().second
        assertTrue(body.contains("Pancakes"))
        assertTrue(body.contains("2.1 cups flour"))
        assertTrue(body.contains("recipe_cleanup"))
        assertTrue(body.contains("mistral-medium-latest"))
    }

    @Test
    fun retriesOnceWhenResponseIsNotValidJson() = runTest {
        val good = """{"ingredients":[{"name":"flour","quantity":"250 g","from":[0]}]}"""
        val transport = CleanupFakeTransport(
            cleanupChatResponse("nope"),
            cleanupChatResponse(good),
        )
        val result = RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("flour"))
        assertEquals(listOf("250 g flour"), result.lines)
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun throwsParseAfterTwoInvalidResponses() = runTest {
        val transport = CleanupFakeTransport(
            cleanupChatResponse("nope"),
            cleanupChatResponse("still not json"),
        )
        try {
            RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("flour"))
            fail("expected mistralParse")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_PARSE, e.code)
        }
    }

    @Test
    fun throwsKeyMissingForBlankKey() = runTest {
        val transport = CleanupFakeTransport()
        try {
            RecipeCleaner(transport).cleanIngredients("  ", "Pancakes", listOf("flour"))
            fail("expected mistralKeyMissing")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_KEY_MISSING, e.code)
        }
    }

    @Test
    fun mapsAuthErrors() = runTest {
        val transport = CleanupFakeTransport(HttpResponse(status = 401, bodyText = "{}"))
        try {
            RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("flour"))
            fail("expected mistralAuth")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_AUTH, e.code)
        }
    }

    @Test
    fun throwsWhenResponseContentIsBlank() = runTest {
        val transport = CleanupFakeTransport(cleanupChatResponse(""))
        try {
            RecipeCleaner(transport).cleanIngredients("key", "Pancakes", listOf("flour"))
            fail("expected mistralEmpty")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_EMPTY, e.code)
        }
    }
}
