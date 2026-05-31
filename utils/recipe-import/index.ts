import { recipeImportErrors } from '@/utils/recipe-import/import-errors';
import { extractRecipeWithMistral } from '@/utils/recipe-import/mistral-client';
import { fetchRecipePageHtml } from '@/utils/recipe-import/native-fetch-url';
import { preprocessRecipeHtml } from '@/utils/recipe-import/preprocess';
import type { NewRecipe } from '@/store/recipes';

export {
  displayRecipeImportError,
  formatRecipeImportError,
  recipeImportErrors,
  type RecipeImportErrorDisplay,
} from '@/utils/recipe-import/import-errors';
export { isValidRecipeUrl } from '@/utils/recipe-import/native-fetch-url';
export {
  importRecipeFromHtmlWithScraper,
  importRecipeFromUrlWithScraper,
} from '@/utils/recipe-import/scraper-import';

export async function importRecipeWithMistral(options: {
  html: string;
  apiKey: string;
  sourceUrl?: string;
  signal?: AbortSignal;
}): Promise<NewRecipe> {
  const trimmed = options.html.trim();
  if (!trimmed) {
    throw recipeImportErrors.emptyContent();
  }
  const preprocessed = preprocessRecipeHtml(trimmed);
  return extractRecipeWithMistral({
    apiKey: options.apiKey,
    preprocessed,
    sourceUrl: options.sourceUrl,
    signal: options.signal,
  });
}

export async function importRecipeFromUrlWithMistral(options: {
  url: string;
  apiKey: string;
  signal?: AbortSignal;
}): Promise<NewRecipe> {
  const html = await fetchRecipePageHtml(options.url, options.signal);
  return importRecipeWithMistral({
    html,
    apiKey: options.apiKey,
    sourceUrl: options.url.trim(),
    signal: options.signal,
  });
}
