package com.conorryan.weeklies.data.import

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreprocessTest {

    @Test
    fun stripsScriptAndStyleTagsFromHtmlSnippet() {
        val html = """
            <html>
              <style>.hidden { display: none; }</style>
              <script>alert("x")</script>
              <body><p>Recipe body</p></body>
            </html>
        """.trimIndent()
        val result = preprocessRecipeHtml(html)
        assertTrue(result.htmlSnippet.contains("Recipe body"))
        assertFalse(result.htmlSnippet.contains("alert"))
        assertFalse(result.htmlSnippet.contains(".hidden"))
    }

    @Test
    fun extractsJsonLdRecipeNodesFromOriginalHtml() {
        val html = """
            <html>
              <script type="application/ld+json">
                {"@type":"Recipe","name":"Pasta","recipeIngredient":["200g pasta"]}
              </script>
              <body>Pasta page</body>
            </html>
        """.trimIndent()
        val result = preprocessRecipeHtml(html)
        assertEquals(1, result.jsonLdRecipes.size)
        assertEquals(
            "Pasta",
            result.jsonLdRecipes[0].jsonObject["name"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun extractsRecipeNodesFromGraph() {
        val html = """
            <script type="application/ld+json">
              {"@graph":[{"@type":"WebPage","name":"Site"},{"@type":"Recipe","name":"Soup"}]}
            </script>
        """.trimIndent()
        val result = preprocessRecipeHtml(html)
        assertEquals(1, result.jsonLdRecipes.size)
        assertEquals(
            "Soup",
            result.jsonLdRecipes[0].jsonObject["name"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun parsesJsonLdFromOriginalHtmlEvenWhenScriptTagsAreStrippedFromSnippet() {
        val html = """
            <script type="application/ld+json">
              {"@type":"Recipe","name":"From LD"}
            </script>
            <p>Visible text only</p>
        """.trimIndent()
        val result = preprocessRecipeHtml(html)
        assertEquals(1, result.jsonLdRecipes.size)
        assertFalse(result.htmlSnippet.contains("From LD"))
        assertTrue(result.htmlSnippet.contains("Visible text only"))
    }

    @Test
    fun capsHtmlSnippetAtMaxHtmlForLlm() {
        val html = "<p>${"x".repeat(ImportConstants.MAX_HTML_FOR_LLM + 500)}</p>"
        val result = preprocessRecipeHtml(html)
        assertTrue(result.htmlSnippet.length <= ImportConstants.MAX_HTML_FOR_LLM)
    }

    @Test
    fun skipsInvalidJsonLdBlocks() {
        val html = """
            <script type="application/ld+json">{not valid json</script>
            <p>content</p>
        """.trimIndent()
        val result = preprocessRecipeHtml(html)
        assertEquals(0, result.jsonLdRecipes.size)
    }
}
