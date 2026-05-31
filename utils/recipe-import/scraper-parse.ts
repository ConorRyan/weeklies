import { extractJsonLdRecipeNodes } from '@/utils/recipe-import/preprocess';
import { recipeImportErrors } from '@/utils/recipe-import/import-errors';

export type ScrapedRecipeData = {
  url?: string;
  name: string;
  recipeYield?: string;
  recipeIngredients: string[];
};

function cleanIngredientLine(line: string): string {
  return line
    .replace(/¼/g, '1/4')
    .replace(/½/g, '1/2')
    .replace(/¾/g, '3/4')
    .replace(/⅔/g, '2/3')
    .replace(/<[^>]*>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function firstString(value: unknown): string | undefined {
  if (typeof value === 'string') {
    return value;
  }
  if (Array.isArray(value) && typeof value[0] === 'string') {
    return value[0];
  }
  return undefined;
}

function ingredientObjectToLine(item: Record<string, unknown>): string | null {
  if (typeof item.name === 'string') {
    const amount = typeof item.value === 'string' ? item.value : undefined;
    const line = amount ? `${amount} ${item.name}` : item.name;
    const cleaned = cleanIngredientLine(line);
    return cleaned.length > 0 ? cleaned : null;
  }
  return null;
}

function transformIngredients(value: unknown): string[] | undefined {
  if (!value || !Array.isArray(value)) {
    return undefined;
  }

  const lines = value
    .map((item) => {
      if (typeof item === 'string') {
        return cleanIngredientLine(item);
      }
      if (item && typeof item === 'object') {
        return ingredientObjectToLine(item as Record<string, unknown>) ?? '';
      }
      return '';
    })
    .filter((line) => line.length > 0);

  return lines.length > 0 ? lines : undefined;
}

function buildScrapedRecipeData(
  props: Record<string, unknown>,
  url?: string
): ScrapedRecipeData | null {
  const name = firstString(props.name)?.trim();
  const recipeYield =
    firstString(props.recipeYield) ?? firstString(props.yield) ?? undefined;
  const recipeIngredients = transformIngredients(
    props.recipeIngredient ??
      props.recipeIngredients ??
      props.ingredients ??
      props.ingredient
  );

  if (!name || !recipeIngredients?.length) {
    return null;
  }

  return { url, name, recipeYield, recipeIngredients };
}

/** Parse recipe data from JSON-LD embedded in HTML. */
export function parseRecipeHtml(
  html: string,
  options: { url?: string } = {}
): ScrapedRecipeData {
  const url = options.url?.trim();

  for (const node of extractJsonLdRecipeNodes(html)) {
    if (!node || typeof node !== 'object') {
      continue;
    }
    const scraped = buildScrapedRecipeData(node as Record<string, unknown>, url);
    if (scraped) {
      return scraped;
    }
  }

  throw recipeImportErrors.noRecipeData();
}
