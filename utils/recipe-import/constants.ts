export const MISTRAL_CHAT_URL = 'https://api.mistral.ai/v1/chat/completions';

export const MISTRAL_RECIPE_MODEL = 'mistral-small-latest';

/** Stored recipes are always normalized to this serving count. */
export const RECIPE_IMPORT_TARGET_SERVINGS = 1;

/** Max characters sent to Mistral after preprocessing. */
export const MAX_HTML_FOR_LLM = 100_000;

export const NATIVE_FETCH_TIMEOUT_MS = 15_000;

export const NATIVE_MAX_RESPONSE_BYTES = 5 * 1024 * 1024;

export const RECIPE_JSON_SCHEMA = {
  type: 'object',
  properties: {
    found: {
      type: 'boolean',
      description:
        'True only when the page contains a cookable food recipe with a real ingredient list.',
    },
    name: { type: 'string' },
    servings: { type: 'number' },
    ingredients: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          quantity: { type: 'string' },
          name: { type: 'string' },
        },
        required: ['name'],
        additionalProperties: false,
      },
    },
  },
  required: ['found', 'name', 'servings', 'ingredients'],
  additionalProperties: false,
} as const;
