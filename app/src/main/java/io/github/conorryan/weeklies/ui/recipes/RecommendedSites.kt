package io.github.conorryan.weeklies.ui.recipes

/**
 * A recipe website offered on the Recipes tab, in the order the app
 * recommends them for import reliability.
 */
data class RecommendedSite(val name: String, val url: String)

/** Recommended recipe websites, best-first. */
val RecommendedSites: List<RecommendedSite> = listOf(
    RecommendedSite("RecipeTin Eats", "https://www.recipetineats.com/"),
    RecommendedSite("Sally's Baking Addiction", "https://sallysbakingaddiction.com/"),
    RecommendedSite("Cookie and Kate", "https://cookieandkate.com/"),
    RecommendedSite("Gimme Some Oven", "https://www.gimmesomeoven.com/"),
    RecommendedSite("Natasha's Kitchen", "https://natashaskitchen.com/"),
    RecommendedSite("Smitten Kitchen", "https://smittenkitchen.com/"),
    RecommendedSite("Love and Lemons", "https://www.loveandlemons.com/"),
    RecommendedSite("Simply Recipes", "https://www.simplyrecipes.com/"),
    RecommendedSite("Serious Eats", "https://www.seriouseats.com/"),
    RecommendedSite("BBC Good Food", "https://www.bbcgoodfood.com/"),
    RecommendedSite("King Arthur Baking", "https://www.kingarthurbaking.com/"),
)
