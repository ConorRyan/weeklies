package com.conorryan.weeklies.data.import

import com.conorryan.weeklies.data.model.Weekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportErrorsTest {

    @Test
    fun canceledErrorsFormatToNull() {
        assertNull(RecipeImportErrors.format(RecipeImportErrors.canceled()))
        assertNull(RecipeImportErrors.format(kotlinx.coroutines.CancellationException("test")))
        assertNull(RecipeImportErrors.format(IllegalStateException("user canceled it")))
    }

    @Test
    fun importExceptionsKeepTheirTitle() {
        val display = RecipeImportErrors.format(RecipeImportErrors.mistralAuth())
        assertEquals("Mistral API key", display?.title)
        assertEquals(
            "Invalid Mistral API key. Check Settings → Recipe import.",
            display?.message,
        )
    }

    @Test
    fun hintsArePreserved() {
        val display = RecipeImportErrors.format(RecipeImportErrors.noRecipeData())
        assertEquals("No recipe found", display?.title)
        assertEquals(true, display?.hint?.contains("Import with AI") ?: false)
    }

    @Test
    fun unknownErrorsGetFallbackCopy() {
        val display = RecipeImportErrors.format(IllegalStateException("boom"))
        assertEquals("Import failed", display?.title)
        assertEquals("boom", display?.message)
    }

    @Test
    fun httpErrorsMapToDistinctCodes() {
        assertEquals(RecipeImportErrorCode.MISTRAL_AUTH, mapMistralHttpError(401).code)
        assertEquals(RecipeImportErrorCode.MISTRAL_RATE_LIMIT, mapMistralHttpError(429).code)
        assertEquals(RecipeImportErrorCode.MISTRAL_UNAVAILABLE, mapMistralHttpError(503).code)
        assertEquals(RecipeImportErrorCode.MISTRAL_FAILED, mapMistralHttpError(404).code)
    }

    @Test
    fun weekdayMappingMatchesJavaDayOfWeek() {
        java.time.DayOfWeek.entries.forEach { dow ->
            assertEquals(dow.name.take(3).lowercase(), Weekday.fromDayOfWeek(dow).key)
        }
    }
}
