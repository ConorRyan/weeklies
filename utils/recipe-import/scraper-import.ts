import type { NewRecipe } from '@/store/recipes';
import { recipeImportErrors } from '@/utils/recipe-import/import-errors';
import { fetchRecipePageHtml } from '@/utils/recipe-import/native-fetch-url';
import { parseRecipeHtml } from '@/utils/recipe-import/scraper-parse';
import { scrapedRecipeToNewRecipe } from '@/utils/recipe-import/scraper-to-recipe';

function assertNotAborted(signal?: AbortSignal): void {
  if (signal?.aborted) {
    throw recipeImportErrors.canceled();
  }
}

export async function importRecipeFromHtmlWithScraper(options: {
  html: string;
  sourceUrl?: string;
  signal?: AbortSignal;
}): Promise<NewRecipe> {
  const trimmed = options.html.trim();
  if (!trimmed) {
    throw recipeImportErrors.emptyContent();
  }

  assertNotAborted(options.signal);

  const scraped = parseRecipeHtml(trimmed, { url: options.sourceUrl?.trim() });

  assertNotAborted(options.signal);

  const recipe = scrapedRecipeToNewRecipe(scraped);
  if (!recipe) {
    throw recipeImportErrors.incompleteRecipe();
  }

  return recipe;
}

export async function importRecipeFromUrlWithScraper(options: {
  url: string;
  signal?: AbortSignal;
}): Promise<NewRecipe> {
  const html = await fetchRecipePageHtml(options.url, options.signal);
  return importRecipeFromHtmlWithScraper({
    html,
    sourceUrl: options.url.trim(),
    signal: options.signal,
  });
}
