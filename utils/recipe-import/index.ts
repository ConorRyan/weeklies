import { extractRecipeWithMistral } from '@/utils/recipe-import/mistral-client';
import { fetchRecipePageHtml } from '@/utils/recipe-import/native-fetch-url';
import { preprocessRecipeHtml } from '@/utils/recipe-import/preprocess';
import type { NewRecipe } from '@/store/recipes';

export { preprocessRecipeHtml } from '@/utils/recipe-import/preprocess';
export { fetchRecipePageHtml, isValidRecipeUrl } from '@/utils/recipe-import/native-fetch-url';
export { extractRecipeWithMistral } from '@/utils/recipe-import/mistral-client';

export async function importRecipeWithMistral(options: {
  html: string;
  apiKey: string;
  sourceUrl?: string;
  signal?: AbortSignal;
}): Promise<NewRecipe> {
  const trimmed = options.html.trim();
  if (!trimmed) {
    throw new Error('Page content is empty.');
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
