import type { NewRecipe } from '@/store/recipes';
import { scaleIngredientLine } from '@/utils/scale-ingredient-line';

export type MistralIngredient = {
  quantity?: string;
  name: string;
};

export type MistralRecipePayload = {
  name: string;
  servings: number;
  ingredients: MistralIngredient[];
};

export function isValidServings(n: number): boolean {
  return Number.isFinite(n) && n > 0;
}

/** Scale ingredient lines from recipe yield to a target number of servings. */
export function scaleIngredientsToServings(
  ingredients: string[],
  recipeServings: number,
  targetServings: number
): string[] {
  if (!isValidServings(recipeServings) || !isValidServings(targetServings)) {
    return ingredients;
  }
  if (recipeServings === targetServings) {
    return ingredients;
  }
  const factor = targetServings / recipeServings;
  return ingredients.map((line) => scaleIngredientLine(line, factor));
}

export function ingredientPartsToLine(quantity: string | undefined, name: string): string | null {
  const n = name.trim();
  if (!n) {
    return null;
  }
  const q = quantity?.trim() ?? '';
  const line = [q, n].filter((s) => s.length > 0).join(' ');
  return line.length > 0 ? line : null;
}

function isMistralIngredient(value: unknown): value is MistralIngredient {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const o = value as Record<string, unknown>;
  return typeof o.name === 'string';
}

export function parseMistralRecipePayload(data: unknown): MistralRecipePayload | null {
  if (!data || typeof data !== 'object') {
    return null;
  }
  const o = data as Record<string, unknown>;
  if (typeof o.name !== 'string' || !Array.isArray(o.ingredients)) {
    return null;
  }
  const ingredients = o.ingredients.filter(isMistralIngredient);
  if (ingredients.length === 0) {
    return null;
  }
  const servings =
    typeof o.servings === 'number'
      ? o.servings
      : typeof o.servings === 'string'
        ? Number(o.servings)
        : NaN;
  if (!isValidServings(servings)) {
    return null;
  }
  return { name: o.name.trim(), servings, ingredients };
}

export function mistralPayloadToNewRecipe(
  payload: MistralRecipePayload,
  targetServings?: number
): NewRecipe | null {
  const name = payload.name.trim();
  if (!name) {
    return null;
  }
  let ingredients = payload.ingredients
    .map((item) => ingredientPartsToLine(item.quantity, item.name))
    .filter((line): line is string => line !== null);
  if (ingredients.length === 0) {
    return null;
  }
  if (targetServings !== undefined && isValidServings(targetServings)) {
    ingredients = scaleIngredientsToServings(ingredients, payload.servings, targetServings);
  }
  return { name, ingredients };
}

export function parseMistralContentJson(content: string): MistralRecipePayload | null {
  try {
    const data = JSON.parse(content) as unknown;
    return parseMistralRecipePayload(data);
  } catch {
    return null;
  }
}
