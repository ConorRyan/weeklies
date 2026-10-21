package io.github.conorryan.weeklies.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private fun fixture(name: String): String =
    object {}.javaClass.getResource("/recipe-import/$name")!!.readText()

private fun htmlWithJsonLd(vararg nodesJson: String): String {
    val blocks = nodesJson.joinToString("") { """<script type="application/ld+json">$it</script>""" }
    return "<html><head>$blocks</head></html>"
}

class ScraperParseTest {

    @Test
    fun parsesJsonLdRecipeFromFixture() {
        val html = fixture("allrecipes-with-jsonld.html")
        val scraped = parseRecipeHtml(html, url = "https://example.com/pasta")

        assertEquals("https://example.com/pasta", scraped.url)
        assertEquals("Tomato Pasta", scraped.name)
        assertEquals("2 servings", scraped.recipeYield)
        assertEquals(listOf("400 g pasta", "2 tomatoes"), scraped.recipeIngredients)
    }

    @Test
    fun throwsWhenPageHasNoSchemaData() {
        val html = fixture("minimal-recipe.html")
        try {
            parseRecipeHtml(html)
            fail("expected noRecipeData")
        } catch (e: RecipeImportException) {
            assertEquals(RecipeImportErrorCode.NO_RECIPE_DATA, e.code)
        }
    }

    @Test
    fun combinesValueAndNameForIngredientObjectsWithAmounts() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"Bread","recipeIngredient":[{"name":"flour","value":"500 g"},{"name":"salt"}]}""",
        )
        val scraped = parseRecipeHtml(html)
        assertEquals("Bread", scraped.name)
        assertEquals(null, scraped.recipeYield)
        assertEquals(listOf("500 g flour", "salt"), scraped.recipeIngredients)
    }

    @Test
    fun cleansUnicodeFractionsAndInlineTagsFromIngredientLines() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"Cake","recipeIngredient":["½ cup <b>sugar</b>","¾ tsp  vanilla"]}""",
        )
        assertEquals(
            listOf("1/2 cup sugar", "3/4 tsp vanilla"),
            parseRecipeHtml(html).recipeIngredients,
        )
    }

    @Test
    fun acceptsArrayOrAlternateYieldFields() {
        val withArray = htmlWithJsonLd(
            """{"@type":"Recipe","name":"A","recipeIngredient":["x"],"recipeYield":["4 servings","2 large"]}""",
        )
        val withYield = htmlWithJsonLd(
            """{"@type":"Recipe","name":"B","recipeIngredient":["x"],"yield":"2 portions"}""",
        )
        assertEquals("4 servings", parseRecipeHtml(withArray).recipeYield)
        assertEquals("2 portions", parseRecipeHtml(withYield).recipeYield)
    }

    @Test
    fun fallsBackToIngredientsOrIngredientPropertyNames() {
        val viaIngredients = htmlWithJsonLd("""{"@type":"Recipe","name":"A","ingredients":["1 onion"]}""")
        val viaIngredient = htmlWithJsonLd("""{"@type":"Recipe","name":"B","ingredient":["1 carrot"]}""")
        assertEquals(listOf("1 onion"), parseRecipeHtml(viaIngredients).recipeIngredients)
        assertEquals(listOf("1 carrot"), parseRecipeHtml(viaIngredient).recipeIngredients)
    }

    @Test
    fun skipsIncompleteRecipeNodesAndUsesTheFirstCompleteOne() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"No ingredients"}""",
            """{"@type":"Recipe","name":"Good","recipeIngredient":["1 egg"]}""",
            """{"@type":"Recipe","name":"Also good","recipeIngredient":["2 eggs"]}""",
        )
        assertEquals("Good", parseRecipeHtml(html).name)
    }

    @Test
    fun trimsTheUrlOptionBeforeAttachingIt() {
        val html = htmlWithJsonLd("""{"@type":"Recipe","name":"A","recipeIngredient":["x"]}""")
        assertEquals("https://example.com/a", parseRecipeHtml(html, url = "  https://example.com/a  ").url)
    }

    @Test
    fun flattensHowToStepInstructions() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"Soup","recipeIngredient":["1 onion"],"recipeInstructions":""" +
                """[{"@type":"HowToStep","text":"Chop the onion."},{"@type":"HowToStep","text":"Simmer 30 <b>minutes</b>."}]}""",
        )
        assertEquals(
            listOf("Chop the onion.", "Simmer 30 minutes."),
            parseRecipeHtml(html).recipeInstructions,
        )
    }

    @Test
    fun flattensHowToSectionGroups() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"Cake","recipeIngredient":["flour"],"recipeInstructions":""" +
                """[{"@type":"HowToSection","name":"Bake","itemListElement":""" +
                """[{"@type":"HowToStep","text":"Mix."},{"@type":"HowToStep","text":"Bake at 180C."}]}]}""",
        )
        assertEquals(listOf("Mix.", "Bake at 180C."), parseRecipeHtml(html).recipeInstructions)
    }

    @Test
    fun acceptsPlainStringInstructions() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"A","recipeIngredient":["x"],"recipeInstructions":"Mix everything and bake."}""",
        )
        assertEquals(listOf("Mix everything and bake."), parseRecipeHtml(html).recipeInstructions)
    }

    @Test
    fun usesStepNameWhenTextIsMissing() {
        val html = htmlWithJsonLd(
            """{"@type":"Recipe","name":"A","recipeIngredient":["x"],"recipeInstructions":""" +
                """[{"@type":"HowToStep","name":"Rest the dough"}]}""",
        )
        assertEquals(listOf("Rest the dough"), parseRecipeHtml(html).recipeInstructions)
    }

    @Test
    fun recipeStaysValidWithoutInstructions() {
        val html = htmlWithJsonLd("""{"@type":"Recipe","name":"A","recipeIngredient":["x"]}""")
        val scraped = parseRecipeHtml(html)
        assertEquals(null, scraped.recipeInstructions)
        assertEquals("A", scraped.name)
    }
}

class ScraperToRecipeTest {

    @Test
    fun parsesPlainNumberYield() {
        assertEquals(4, parseRecipeYield("4"))
    }

    @Test
    fun parsesServingsYield() {
        assertEquals(4, parseRecipeYield("4 servings"))
    }

    @Test
    fun parsesServesYield() {
        assertEquals(6, parseRecipeYield("Serves 6"))
    }

    @Test
    fun usesFirstNumberInARange() {
        assertEquals(6, parseRecipeYield("6-8 servings"))
    }

    @Test
    fun returnsNullForEmptyOrUnparseableValues() {
        assertEquals(null, parseRecipeYield(""))
        assertEquals(null, parseRecipeYield(null))
        assertEquals(null, parseRecipeYield("no yield"))
        assertEquals(null, parseRecipeYield("0"))
    }

    @Test
    fun scalesIngredientsFromRecipeYieldToOnePortion() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = "4 servings",
                recipeIngredients = listOf("2 cups flour", "4 eggs"),
            ),
        )
        assertEquals("Pasta", result?.name)
        assertEquals(listOf("0.5 cups flour", "1 eggs"), result?.ingredients)
    }

    @Test
    fun returnsIngredientsUnchangedWhenYieldIsMissing() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = null,
                recipeIngredients = listOf("2 cups flour"),
            ),
        )
        assertEquals(listOf("2 cups flour"), result?.ingredients)
    }

    @Test
    fun returnsNullWhenNameIsMissing() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "",
                recipeYield = null,
                recipeIngredients = listOf("2 cups flour"),
            ),
        )
        assertEquals(null, result)
    }

    @Test
    fun returnsNullWhenIngredientsAreEmpty() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = null,
                recipeIngredients = emptyList(),
            ),
        )
        assertEquals(null, result)
    }

    @Test
    fun rejectsZeroYieldScaling() {
        // A yield of "0" parses to null servings, so no scaling happens.
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = "0",
                recipeIngredients = listOf("2 cups flour"),
            ),
        )
        assertTrue(result != null)
    }

    @Test
    fun joinsInstructionsIntoOneBlob() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = null,
                recipeIngredients = listOf("2 cups flour"),
                recipeInstructions = listOf("Mix.", "Bake."),
            ),
        )
        assertEquals("Mix.\n\nBake.", result?.instructions)
    }

    @Test
    fun defaultsInstructionsToEmptyBlob() {
        val result = scrapedRecipeToNewRecipe(
            ScrapedRecipeData(
                url = null,
                name = "Pasta",
                recipeYield = null,
                recipeIngredients = listOf("2 cups flour"),
            ),
        )
        assertEquals("", result?.instructions)
    }
}
