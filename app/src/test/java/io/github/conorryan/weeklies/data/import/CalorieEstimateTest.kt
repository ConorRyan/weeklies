package io.github.conorryan.weeklies.data.import

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Fake transport returning queued responses, recording requests. */
private class FakeChatTransport(vararg responses: HttpResponse) : HttpTransport {
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

class CalorieEstimateParseTest {

    @Test
    fun parsesValidEstimate() {
        val estimate = parseCalorieEstimateResponse(
            """{"calories":550,"confidence":"high","rationale":"Oil and pasta dominate."}""",
        )
        assertEquals(CalorieEstimate(550, CalorieConfidence.HIGH, "Oil and pasta dominate."), estimate)
    }

    @Test
    fun uppercaseConfidenceStillMatches() {
        val estimate = parseCalorieEstimateResponse("""{"calories":300,"confidence":"LOW","rationale":""}""")
        assertEquals(CalorieConfidence.LOW, estimate?.confidence)
        assertEquals("", estimate?.rationale)
    }

    @Test
    fun unknownConfidenceDegradesToMedium() {
        val estimate = parseCalorieEstimateResponse("""{"calories":300,"confidence":"maybe","rationale":"x"}""")
        assertEquals(CalorieConfidence.MEDIUM, estimate?.confidence)
    }

    @Test
    fun missingConfidenceDegradesToMedium() {
        val estimate = parseCalorieEstimateResponse("""{"calories":300,"rationale":"x"}""")
        assertEquals(CalorieConfidence.MEDIUM, estimate?.confidence)
    }

    @Test
    fun fractionalCaloriesRoundToWholeNumbers() {
        assertEquals(300, parseCalorieEstimateResponse("""{"calories":300.4,"confidence":"high","rationale":""}""")?.calories)
    }

    @Test
    fun implausibleCaloriesAreRejected() {
        assertNull(parseCalorieEstimateResponse("""{"calories":0,"confidence":"high","rationale":""}"""))
        assertNull(parseCalorieEstimateResponse("""{"calories":49,"confidence":"high","rationale":""}"""))
        assertNull(parseCalorieEstimateResponse("""{"calories":5001,"confidence":"high","rationale":""}"""))
    }

    @Test
    fun invalidJsonOrShapeIsRejected() {
        assertNull(parseCalorieEstimateResponse("not json"))
        assertNull(parseCalorieEstimateResponse("""{"calories":"lots","confidence":"high","rationale":""}"""))
        assertNull(parseCalorieEstimateResponse("""{}"""))
    }
}

class CalorieEstimatorTest {

    @Test
    fun estimatesFromRecipeContent() = runTest {
        val content = """{"calories":550,"confidence":"medium","rationale":"Olive oil carries most energy."}"""
        val transport = FakeChatTransport(chatResponse(content))
        val estimate = CalorieEstimator(transport).estimate("key", "Pasta", listOf("200 g pasta", "2 tbsp olive oil"), "- Boil.")

        assertEquals(CalorieEstimate(550, CalorieConfidence.MEDIUM, "Olive oil carries most energy."), estimate)
        val body = transport.requests.single().second
        assertTrue(body.contains(ImportConstants.MISTRAL_MODEL))
        assertTrue(body.contains("calorie_estimate"))
        assertTrue(body.contains("You estimate the calories of a recipe portion"))
        assertTrue(body.contains("Recipe: Pasta"))
        assertTrue(body.contains("- 200 g pasta"))
        assertTrue(body.contains("- Boil."))
    }

    @Test
    fun omitsInstructionsSectionWhenBlank() = runTest {
        val content = """{"calories":400,"confidence":"low","rationale":"Sparse amounts."}"""
        val transport = FakeChatTransport(chatResponse(content))
        CalorieEstimator(transport).estimate("key", "Soup", listOf("1 onion"), "")
        val body = transport.requests.single().second
        assertTrue(!body.contains("Instructions:"))
    }

    @Test
    fun retriesOnceWhenEstimateIsImplausible() = runTest {
        val transport = FakeChatTransport(
            chatResponse("""{"calories":99999,"confidence":"high","rationale":""}"""),
            chatResponse("""{"calories":550,"confidence":"high","rationale":"Fixed."}"""),
        )
        val estimate = CalorieEstimator(transport).estimate("key", "Pasta", listOf("200 g pasta"), "")
        assertEquals(550, estimate.calories)
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun throwsKeyMissingForBlankApiKey() = runTest {
        val transport = FakeChatTransport()
        try {
            CalorieEstimator(transport).estimate("  ", "Pasta", listOf("200 g pasta"), "")
            fail("expected key missing")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_KEY_MISSING, e.code)
        }
        assertEquals(0, transport.requests.size)
    }

    @Test
    fun mapsHttpErrorsToRecipeImportErrors() = runTest {
        val transport = FakeChatTransport(HttpResponse(429, "{}"))
        try {
            CalorieEstimator(transport).estimate("key", "Pasta", listOf("200 g pasta"), "")
            fail("expected rate limit")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_RATE_LIMIT, e.code)
        }
    }
}
