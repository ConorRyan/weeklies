package io.github.conorryan.weeklies.data.import

import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Weekday
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
        assertEquals(R.string.import_error_mistral_auth, display?.title)
        assertEquals(R.string.import_msg_mistral_auth, display?.message)
    }

    @Test
    fun hintsArePreserved() {
        val display = RecipeImportErrors.format(RecipeImportErrors.noRecipeData())
        assertEquals(R.string.import_error_no_recipe_data, display?.title)
        assertEquals(R.string.import_hint_no_recipe_data, display?.hint)
    }

    @Test
    fun unknownErrorsGetFallbackCopy() {
        val display = RecipeImportErrors.format(IllegalStateException("boom"))
        assertEquals(R.string.import_error_unknown, display?.title)
        assertEquals(R.string.import_msg_unknown, display?.message)
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
