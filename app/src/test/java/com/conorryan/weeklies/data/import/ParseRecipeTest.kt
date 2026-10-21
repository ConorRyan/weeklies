package com.conorryan.weeklies.data.import

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseRecipeTest {

    @Test
    fun acceptsPositiveFiniteServings() {
        assertTrue(isValidServings(4.0))
    }

    @Test
    fun rejectsZeroAndNegativeServings() {
        assertFalse(isValidServings(0.0))
        assertFalse(isValidServings(-1.0))
    }

    @Test
    fun rejectsNonFiniteServings() {
        assertFalse(isValidServings(Double.NaN))
    }

    @Test
    fun scalesIngredientsFromFourServingsToOne() {
        val result = scaleIngredientsToServings(listOf("2 cups flour"), 4.0, 1.0)
        assertEquals("0.5 cups flour", result[0])
    }

    @Test
    fun returnsUnchangedWhenServingsInvalid() {
        val ingredients = listOf("2 cups flour")
        assertEquals(ingredients, scaleIngredientsToServings(ingredients, 0.0, 1.0))
    }

    @Test
    fun joinsQuantityAndName() {
        assertEquals("200 g flour", ingredientPartsToLine("200 g", "flour"))
    }

    @Test
    fun returnsNullForEmptyIngredientName() {
        assertNull(ingredientPartsToLine("200 g", "  "))
    }

    @Test
    fun handlesMissingQuantity() {
        assertEquals("salt", ingredientPartsToLine(null, "salt"))
    }

    private fun parsePayload(json: String) = parseMistralRecipePayload(Json.parseToJsonElement(json))

    @Test
    fun parsesValidPayload() {
        val result = parsePayload(
            """{"name":"Pasta","servings":4,"ingredients":[{"name":"pasta","quantity":"200 g"}]}""",
        )
        assertEquals("Pasta", result?.name)
        assertEquals(4.0, result?.servings ?: 0.0, 1e-9)
        assertEquals(1, result?.ingredients?.size)
        assertEquals("200 g", result?.ingredients?.first()?.quantity)
        assertEquals("pasta", result?.ingredients?.first()?.name)
    }

    @Test
    fun parsesStringServings() {
        val result = parsePayload(
            """{"name":"Pasta","servings":"4","ingredients":[{"name":"pasta"}]}""",
        )
        assertEquals(4.0, result?.servings ?: 0.0, 1e-9)
    }

    @Test
    fun returnsNullForInvalidPayload() {
        assertNull(parseMistralRecipePayload(null))
        assertNull(parsePayload("""{"name":"X","servings":0,"ingredients":[]}"""))
        assertNull(parsePayload("""{"name":"X","servings":2,"ingredients":[{"noName":true}]}"""))
    }

    @Test
    fun buildsRecipeScaledToTargetServings() {
        val payload = parsePayload(
            """{"name":"Pasta","servings":4,"ingredients":[{"name":"pasta","quantity":"400 g"}]}""",
        )!!
        val result = mistralPayloadToNewRecipe(payload, 1)
        assertEquals("Pasta", result?.name)
        assertEquals(listOf("100 g pasta"), result?.ingredients)
    }

    @Test
    fun returnsNullForEmptyName() {
        val payload = parsePayload(
            """{"name":"  ","servings":2,"ingredients":[{"name":"salt"}]}""",
        )!!
        assertNull(mistralPayloadToNewRecipe(payload, 1))
    }

    @Test
    fun returnsNullWhenNoValidIngredients() {
        val payload = parsePayload(
            """{"name":"Pasta","servings":2,"ingredients":[{"name":"  "}]}""",
        )!!
        assertNull(mistralPayloadToNewRecipe(payload, 1))
    }

    @Test
    fun returnsNoRecipeWhenFoundIsFalse() {
        val content = """{"found":false,"name":"","servings":1,"ingredients":[]}"""
        assertTrue(parseMistralResponse(content) is MistralParseOutcome.NoRecipe)
    }

    @Test
    fun returnsRecipePayloadWhenFoundIsTrue() {
        val content = """{"found":true,"name":"Soup","servings":2,"ingredients":[{"name":"water"}]}"""
        val result = parseMistralResponse(content)
        assertTrue(result is MistralParseOutcome.Recipe)
        assertEquals("Soup", (result as MistralParseOutcome.Recipe).payload.name)
    }

    private fun fixtureText(name: String): String =
        object {}.javaClass.getResource("/recipe-import/$name")!!.readText()
}
