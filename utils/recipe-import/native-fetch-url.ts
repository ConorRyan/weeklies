import { NATIVE_FETCH_TIMEOUT_MS, NATIVE_MAX_RESPONSE_BYTES } from '@/utils/recipe-import/constants';

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
    throw new Error('Enter a valid http or https URL.');
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), NATIVE_FETCH_TIMEOUT_MS);

  const onAbort = () => controller.abort();
  if (signal) {
    if (signal.aborted) {
      clearTimeout(timeoutId);
      throw new Error('Import canceled.');
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
      throw new Error(`Could not load page (${response.status}).`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      const text = await response.text();
      if (text.length > NATIVE_MAX_RESPONSE_BYTES) {
        throw new Error('Page is too large to import.');
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
          throw new Error('Page is too large to import.');
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
    if (e instanceof Error) {
      if (e.name === 'AbortError') {
        throw new Error('Import canceled.');
      }
      if (e.message.includes('Could not load') || e.message.includes('too large') || e.message.includes('valid http')) {
        throw e;
      }
    }
    throw new Error('Could not load page. Check the URL and your connection.');
  } finally {
    clearTimeout(timeoutId);
    if (signal) {
      signal.removeEventListener('abort', onAbort);
    }
  }
}
