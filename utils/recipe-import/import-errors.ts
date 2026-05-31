export type RecipeImportErrorCode =
  | 'canceled'
  | 'empty_content'
  | 'invalid_url'
  | 'no_recipe_data'
  | 'incomplete_recipe'
  | 'parse_failed'
  | 'fetch_failed'
  | 'page_too_large'
  | 'mistral_auth'
  | 'mistral_rate_limit'
  | 'mistral_unavailable'
  | 'mistral_failed'
  | 'mistral_empty'
  | 'mistral_parse'
  | 'mistral_key_missing'
  | 'unknown';

export type RecipeImportErrorDisplay = {
  title: string;
  message: string;
  hint?: string;
};

export class RecipeImportError extends Error {
  readonly code: RecipeImportErrorCode;
  readonly hint?: string;

  constructor(message: string, code: RecipeImportErrorCode, hint?: string) {
    super(message);
    this.name = 'RecipeImportError';
    this.code = code;
    this.hint = hint;
  }
}

function errorText(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '';
}

function isAbortError(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'name' in error &&
    (error as { name: string }).name === 'AbortError'
  );
}

export function isImportCanceled(error: unknown): boolean {
  if (error instanceof RecipeImportError && error.code === 'canceled') {
    return true;
  }
  if (isAbortError(error)) {
    return true;
  }
  const lower = errorText(error).toLowerCase();
  return lower.includes('cancel');
}

const ERROR_TITLES: Record<RecipeImportErrorCode, string> = {
  canceled: 'Import canceled',
  empty_content: 'Nothing to import',
  invalid_url: 'Invalid URL',
  no_recipe_data: 'No recipe found',
  incomplete_recipe: 'Incomplete recipe',
  parse_failed: 'Could not read page',
  fetch_failed: 'Could not load page',
  page_too_large: 'Page too large',
  mistral_auth: 'Mistral API key',
  mistral_rate_limit: 'Rate limit',
  mistral_unavailable: 'Mistral unavailable',
  mistral_failed: 'Import with AI failed',
  mistral_empty: 'Import with AI failed',
  mistral_parse: 'Import with AI failed',
  mistral_key_missing: 'Mistral API key',
  unknown: 'Import failed',
};

export const recipeImportErrors = {
  canceled: () => new RecipeImportError('Import canceled.', 'canceled'),

  emptyContent: () =>
    new RecipeImportError('Paste or load page content before importing.', 'empty_content'),

  missingUrl: () => new RecipeImportError('Enter a recipe URL.', 'empty_content'),

  invalidUrl: () =>
    new RecipeImportError('Enter a valid http or https URL.', 'invalid_url'),

  noRecipeData: () =>
    new RecipeImportError(
      "This page doesn't include recipe data we can read.",
      'no_recipe_data',
      'Try a different recipe page, paste the full page HTML on web, or use Import with AI.'
    ),

  incompleteRecipe: () =>
    new RecipeImportError(
      'The page had recipe markup but no usable name and ingredients.',
      'incomplete_recipe',
      'Try Import with AI or create the recipe manually.'
    ),

  parseFailed: () =>
    new RecipeImportError(
      'Could not parse the page HTML.',
      'parse_failed',
      'Make sure you copied the full page source, not just the visible text.'
    ),

  fetchFailed: (status?: number) =>
    new RecipeImportError(
      status ? `Could not load page (HTTP ${status}).` : 'Could not load page.',
      'fetch_failed',
      'Check the URL, your connection, and that the site allows fetching this page.'
    ),

  pageTooLarge: () =>
    new RecipeImportError(
      'Page is too large to import.',
      'page_too_large',
      'Try a shorter recipe page or paste the HTML on web.'
    ),

  mistralKeyMissing: () =>
    new RecipeImportError(
      'Add your Mistral API key in Settings → Recipe import.',
      'mistral_key_missing',
      'Import with AI needs a key; standard Import does not.'
    ),

  mistralAuth: () =>
    new RecipeImportError(
      'Invalid Mistral API key. Check Settings → Recipe import.',
      'mistral_auth'
    ),

  mistralRateLimit: () =>
    new RecipeImportError(
      'Mistral rate limit reached. Try again later.',
      'mistral_rate_limit'
    ),

  mistralUnavailable: () =>
    new RecipeImportError(
      'Mistral is temporarily unavailable. Try again later.',
      'mistral_unavailable'
    ),

  mistralFailed: (status: number) =>
    new RecipeImportError(`Mistral request failed (${status}).`, 'mistral_failed'),

  mistralEmpty: () =>
    new RecipeImportError(
      'Mistral returned an empty response.',
      'mistral_empty',
      'Try again, or use standard Import if the page has JSON-LD recipe data.'
    ),

  mistralParse: () =>
    new RecipeImportError(
      'Could not parse recipe from Mistral response.',
      'mistral_parse',
      'Try again, or use standard Import if the page has JSON-LD recipe data.'
    ),

  unknown: () =>
    new RecipeImportError(
      'Something went wrong while importing.',
      'unknown',
      'Try again, or use Import with AI if standard Import cannot read the page.'
    ),
};

export function displayRecipeImportError(error: RecipeImportError): RecipeImportErrorDisplay {
  return {
    title: ERROR_TITLES[error.code],
    message: error.message,
    hint: error.hint,
  };
}

export function formatRecipeImportError(error: unknown): RecipeImportErrorDisplay | null {
  if (isImportCanceled(error)) {
    return null;
  }

  if (error instanceof RecipeImportError) {
    return displayRecipeImportError(error);
  }

  const message = errorText(error) || recipeImportErrors.unknown().message;
  return {
    title: ERROR_TITLES.unknown,
    message,
    hint: recipeImportErrors.unknown().hint,
  };
}
