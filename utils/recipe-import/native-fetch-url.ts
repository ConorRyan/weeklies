import { NATIVE_FETCH_TIMEOUT_MS, NATIVE_MAX_RESPONSE_BYTES } from '@/utils/recipe-import/constants';
import { RecipeImportError, recipeImportErrors } from '@/utils/recipe-import/import-errors';

export type FetchFn = typeof fetch;

export function isValidRecipeUrl(urlString: string): boolean {
  try {
    const u = new URL(urlString.trim());
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}

export async function fetchRecipePageHtml(
  url: string,
  signal?: AbortSignal,
  fetchFn: FetchFn = fetch
): Promise<string> {
  const trimmed = url.trim();
  if (!isValidRecipeUrl(trimmed)) {
    throw recipeImportErrors.invalidUrl();
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), NATIVE_FETCH_TIMEOUT_MS);

  const onAbort = () => controller.abort();
  if (signal) {
    if (signal.aborted) {
      clearTimeout(timeoutId);
      throw recipeImportErrors.canceled();
    }
    signal.addEventListener('abort', onAbort);
  }

  try {
    const response = await fetchFn(trimmed, {
      method: 'GET',
      headers: {
        Accept: 'text/html,application/xhtml+xml;q=0.9,*/*;q=0.8',
        'User-Agent': 'Weeklies/1.0 (recipe import)',
      },
      signal: controller.signal,
    });

    if (!response.ok) {
      throw recipeImportErrors.fetchFailed(response.status);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      const text = await response.text();
      if (text.length > NATIVE_MAX_RESPONSE_BYTES) {
        throw recipeImportErrors.pageTooLarge();
      }
      return text;
    }

    const chunks: Uint8Array[] = [];
    let total = 0;
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      if (value) {
        total += value.length;
        if (total > NATIVE_MAX_RESPONSE_BYTES) {
          throw recipeImportErrors.pageTooLarge();
        }
        chunks.push(value);
      }
    }

    const combined = new Uint8Array(total);
    let offset = 0;
    for (const chunk of chunks) {
      combined.set(chunk, offset);
      offset += chunk.length;
    }
    return new TextDecoder('utf-8', { fatal: false }).decode(combined);
  } catch (e) {
    if (e instanceof RecipeImportError) {
      throw e;
    }
    if (e instanceof Error && e.name === 'AbortError') {
      throw recipeImportErrors.canceled();
    }
    throw recipeImportErrors.fetchFailed();
  } finally {
    clearTimeout(timeoutId);
    if (signal) {
      signal.removeEventListener('abort', onAbort);
    }
  }
}
