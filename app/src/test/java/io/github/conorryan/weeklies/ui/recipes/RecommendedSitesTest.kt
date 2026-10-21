package io.github.conorryan.weeklies.ui.recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedSitesTest {

    @Test
    fun sitesAreOrderedBestFirst() {
        assertEquals(
            listOf(
                "RecipeTin Eats",
                "Sally's Baking Addiction",
                "Cookie and Kate",
                "Gimme Some Oven",
                "Natasha's Kitchen",
                "Smitten Kitchen",
                "Love and Lemons",
                "Simply Recipes",
                "Serious Eats",
                "BBC Good Food",
                "King Arthur Baking",
            ),
            RecommendedSites.map { it.name },
        )
    }

    @Test
    fun urlsAreValidHttps() {
        for (site in RecommendedSites) {
            assertTrue(site.url.startsWith("https://"))
            val uri = java.net.URI(site.url)
            assertTrue(uri.host.isNotEmpty())
            assertNull(uri.fragment)
        }
    }

    @Test
    fun namesAreUnique() {
        assertEquals(RecommendedSites.size, RecommendedSites.map { it.name }.toSet().size)
    }
}
