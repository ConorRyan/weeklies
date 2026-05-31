import type { ScrapedRecipeData } from '@/utils/recipe-import/scraper-parse';

import type { NewRecipe } from '@/store/recipes';
import { RECIPE_IMPORT_TARGET_SERVINGS } from '@/utils/recipe-import/constants';
import { isValidServings, scaleIngredientsToServings } from '@/utils/recipe-import/parse-recipe';

/** Extract a positive serving count from recipeYield text (uses first number found). */
export function parseRecipeYield(raw?: string): number | null {
  if (!raw?.trim()) {
    return null;
  }
  const match = raw.match(/(\d+(?:\.\d+)?)/);
  if (!match) {
    return null;
  }
  const n = Number(match[1]);
  return isValidServings(n) ? n : null;
}

export function scrapedRecipeToNewRecipe(
  data: ScrapedRecipeData,
  targetServings: number = RECIPE_IMPORT_TARGET_SERVINGS
): NewRecipe | null {
  const name = data.name?.trim() ?? '';
  if (!name) {
    return null;
  }

  const ingredients = (data.recipeIngredients ?? [])
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
  if (ingredients.length === 0) {
    return null;
  }

  const recipeServings = parseRecipeYield(data.recipeYield);
  if (recipeServings !== null && isValidServings(targetServings)) {
    return {
      name,
      ingredients: scaleIngredientsToServings(ingredients, recipeServings, targetServings),
    };
  }

  return { name, ingredients };
}
