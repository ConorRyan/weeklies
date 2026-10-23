package io.github.conorryan.weeklies.data.import

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The shared wire protocol, tested through MistralChat's own interface. */
class MistralChatTest {

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
        bodyText = """{"choices":[{"message":{"content":${JsonPrimitive(content)}}}]}""",
    )

    private val schema: JsonObject = buildJsonObject { put("type", "object") }

    @Test
    fun sendsStrictSchemaRequestBody() = runTest {
        val transport = FakeTransport(chatResponse("""{"ok":true}"""))
        val result = MistralChat(transport).chatForJson(
            apiKey = "key",
            model = "model-x",
            schemaName = "schema-name",
            systemPrompt = "system prompt",
            userContent = "user prompt",
            schema = schema,
        ) { it }
        assertEquals("""{"ok":true}""", result)

        val (method, body) = transport.requests.single()
        assertEquals("POST", method)
        val request = Json.parseToJsonElement(body) as JsonObject
        assertEquals("model-x", (request["model"] as JsonPrimitive).content)
        assertEquals("0.1", (request["temperature"] as JsonPrimitive).content)
        val responseFormat = request["response_format"] as JsonObject
        assertEquals("json_schema", (responseFormat["type"] as JsonPrimitive).content)
        val jsonSchema = responseFormat["json_schema"] as JsonObject
        assertEquals("schema-name", (jsonSchema["name"] as JsonPrimitive).content)
        assertEquals("true", (jsonSchema["strict"] as JsonPrimitive).content)
    }

    @Test
    fun mapsHttpErrorsToImportExceptions() = runTest {
        for ((status, code) in listOf(401 to RecipeImportErrorCode.MISTRAL_AUTH, 429 to RecipeImportErrorCode.MISTRAL_RATE_LIMIT)) {
            val transport = FakeTransport(HttpResponse(status, "{}"))
            try {
                MistralChat(transport).chatForJson("key", "m", "s", "sys", "user", schema) { it }
                fail("expected $status to throw")
            } catch (e: RecipeImportException) {
                assertEquals(code, e.code)
            }
        }
    }

    @Test
    fun throwsOnEmptyContent() = runTest {
        val transport = FakeTransport(HttpResponse(200, """{"choices":[{"message":{"content":""}}]}"""))
        try {
            MistralChat(transport).chatForJson("key", "m", "s", "sys", "user", schema) { it }
            fail("expected mistralEmpty")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_EMPTY, e.code)
        }
    }

    @Test
    fun retriesOnceWhenParseFailsThenSucceeds() = runTest {
        val transport = FakeTransport(
            chatResponse("not json"),
            chatResponse("""{"ok":true}"""),
        )
        var parses = 0
        val result = MistralChat(transport).chatForJson("key", "m", "s", "sys", "user", schema) { content ->
            parses++
            if (content == "not json") null else content
        }
        assertEquals("""{"ok":true}""", result)
        assertEquals(2, parses)
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun parseFailureOnRetryThrowsMistralParse() = runTest {
        val transport = FakeTransport(chatResponse("bad"), chatResponse("still bad"))
        try {
            MistralChat(transport).chatForJson("key", "m", "s", "sys", "user", schema) { null }
            fail("expected mistralParse")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.MISTRAL_PARSE, e.code)
        }
        assertEquals(2, transport.requests.size)
    }

    @Test
    fun parseCanAbortWithoutRetryByThrowing() = runTest {
        val transport = FakeTransport(chatResponse("""{"found":false}"""))
        try {
            MistralChat(transport).chatForJson("key", "m", "s", "sys", "user", schema) { content ->
                throw RecipeImportErrors.noRecipeData()
            }
            fail("expected noRecipeData")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.NO_RECIPE_DATA, e.code)
        }
        assertTrue(transport.requests.size == 1)
    }
}
