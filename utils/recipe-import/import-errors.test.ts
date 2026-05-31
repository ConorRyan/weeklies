import {
  displayRecipeImportError,
  formatRecipeImportError,
  isImportCanceled,
  recipeImportErrors,
  RecipeImportError,
} from '@/utils/recipe-import/import-errors';

describe('isImportCanceled', () => {
  it('detects RecipeImportError canceled code', () => {
    expect(isImportCanceled(recipeImportErrors.canceled())).toBe(true);
  });

  it('detects AbortError', () => {
    const err = new Error('aborted');
    err.name = 'AbortError';
    expect(isImportCanceled(err)).toBe(true);
  });
});

describe('formatRecipeImportError', () => {
  it('returns null for canceled errors', () => {
    expect(formatRecipeImportError(recipeImportErrors.canceled())).toBeNull();
  });

  it('formats structured import errors with hints', () => {
    const formatted = formatRecipeImportError(recipeImportErrors.noRecipeData());
    expect(formatted).toEqual({
      title: 'No recipe found',
      message: "This page doesn't include recipe data we can read.",
      hint: 'Try a different recipe page, paste the full page HTML on web, or use Import with AI.',
    });
  });

  it('formats unknown errors', () => {
    const formatted = formatRecipeImportError(new Error('Network down'));
    expect(formatted?.title).toBe('Import failed');
    expect(formatted?.message).toBe('Network down');
  });
});

describe('displayRecipeImportError', () => {
  it('maps error codes to titles', () => {
    expect(displayRecipeImportError(recipeImportErrors.missingUrl())).toEqual({
      title: 'Nothing to import',
      message: 'Enter a recipe URL.',
    });
  });
});

describe('RecipeImportError', () => {
  it('exposes code and hint', () => {
    const error = recipeImportErrors.fetchFailed(404);
    expect(error).toBeInstanceOf(RecipeImportError);
    expect(error.code).toBe('fetch_failed');
    expect(error.message).toContain('HTTP 404');
    expect(error.hint).toContain('Check the URL');
  });
});
