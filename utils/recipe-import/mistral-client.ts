import {
  MISTRAL_CHAT_URL,
  MISTRAL_RECIPE_MODEL,
  RECIPE_IMPORT_TARGET_SERVINGS,
  RECIPE_JSON_SCHEMA,
} from '@/utils/recipe-import/constants';
import {
  mistralPayloadToNewRecipe,
  parseMistralContentJson,
  type MistralRecipePayload,
} from '@/utils/recipe-import/parse-recipe';
import type { FetchFn } from '@/utils/recipe-import/native-fetch-url';
import type { PreprocessedRecipePage } from '@/utils/recipe-import/preprocess';
import type { NewRecipe } from '@/store/recipes';

const SYSTEM_PROMPT = `You extract recipe data from web page content.
Return only a JSON object matching the schema: recipe title, servings (yield), and ingredients.
"servings" is how many people the listed ingredient amounts are for (e.g. from "Serves 4", recipeYield, or portion count). Use a positive number; if only a range is given, use the lower number.
Each ingredient has "name" (required) and optional "quantity" (amount and unit, e.g. "200 g", "1/2 cup").
Do not include cooking steps, notes, or equipment.
If quantities appear in the source, put them in "quantity" so they can be scaled later.
Prefer fewer, accurate lines over guessing.`;

function buildUserMessage(
  preprocessed: PreprocessedRecipePage,
  sourceUrl?: string
): string {
  const parts: string[] = [];
  if (sourceUrl) {
    parts.push(`Source URL: ${sourceUrl}`);
  }
  if (preprocessed.jsonLdRecipes.length > 0) {
    parts.push(
      `JSON-LD Recipe data:\n${JSON.stringify(preprocessed.jsonLdRecipes.slice(0, 3))}`
    );
  }
  parts.push(`Page content:\n${preprocessed.htmlSnippet}`);
  return parts.join('\n\n');
}

export function mapMistralHttpError(status: number): string {
  if (status === 401) {
    return 'Invalid Mistral API key. Check Settings → Recipe import.';
  }
  if (status === 429) {
    return 'Mistral rate limit reached. Try again later.';
  }
  if (status >= 500) {
    return 'Mistral is temporarily unavailable. Try again later.';
  }
  return `Mistral request failed (${status}).`;
}

async function callMistralOnce(
  apiKey: string,
  preprocessed: PreprocessedRecipePage,
  sourceUrl: string | undefined,
  signal: AbortSignal | undefined,
  retryFixJson: boolean,
  fetchFn: FetchFn
): Promise<string> {
  const userContent = retryFixJson
    ? 'Your previous response was not valid JSON for the schema. Reply with only valid JSON for the recipe schema, no markdown.'
    : buildUserMessage(preprocessed, sourceUrl);

  const response = await fetchFn(MISTRAL_CHAT_URL, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    signal,
    body: JSON.stringify({
      model: MISTRAL_RECIPE_MODEL,
      temperature: 0.1,
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: userContent },
      ],
      response_format: {
        type: 'json_schema',
        json_schema: {
          name: 'recipe_extract',
          strict: true,
          schema: RECIPE_JSON_SCHEMA,
        },
      },
    }),
  });

  if (!response.ok) {
    throw new Error(mapMistralHttpError(response.status));
  }

  const body = (await response.json()) as {
    choices?: { message?: { content?: string } }[];
  };
  const content = body.choices?.[0]?.message?.content;
  if (typeof content !== 'string' || content.trim() === '') {
    throw new Error('Mistral returned an empty response.');
  }
  return content;
}

function payloadToRecipe(payload: MistralRecipePayload, targetServings: number): NewRecipe {
  const recipe = mistralPayloadToNewRecipe(payload, targetServings);
  if (!recipe) {
    throw new Error('Could not parse recipe from Mistral response.');
  }
  return recipe;
}

export async function extractRecipeWithMistral(
  options: {
    apiKey: string;
    preprocessed: PreprocessedRecipePage;
    sourceUrl?: string;
    signal?: AbortSignal;
  },
  fetchFn: FetchFn = fetch
): Promise<NewRecipe> {
  const key = options.apiKey.trim();
  if (!key) {
    throw new Error('Add your Mistral API key in Settings → Recipe import.');
  }

  let content = await callMistralOnce(
    key,
    options.preprocessed,
    options.sourceUrl,
    options.signal,
    false,
    fetchFn
  );

  let payload = parseMistralContentJson(content);
  if (!payload) {
    content = await callMistralOnce(
      key,
      options.preprocessed,
      options.sourceUrl,
      options.signal,
      true,
      fetchFn
    );
    payload = parseMistralContentJson(content);
  }

  if (!payload) {
    throw new Error('Could not parse recipe from Mistral response.');
  }

  return payloadToRecipe(payload, RECIPE_IMPORT_TARGET_SERVINGS);
}
