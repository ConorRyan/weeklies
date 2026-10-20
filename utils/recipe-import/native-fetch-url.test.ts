import { readFileSync } from 'fs';
import { join } from 'path';

import { NATIVE_FETCH_TIMEOUT_MS, NATIVE_MAX_RESPONSE_BYTES } from '@/utils/recipe-import/constants';
import { fetchRecipePageHtml, type FetchFn } from '@/utils/recipe-import/native-fetch-url';

function fixture(name: string): string {
  return readFileSync(join(__dirname, '__fixtures__', name), 'utf-8');
}

function mockResponse(options: {
  ok?: boolean;
  status?: number;
  text?: string;
  body?: ReadableStream<Uint8Array> | null;
}): Response {
  return {
    ok: options.ok ?? true,
    status: options.status ?? 200,
    text: async () => options.text ?? '',
    body: options.body ?? null,
  } as Response;
}

describe('fetchRecipePageHtml', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  it('returns html on success without stream reader', async () => {
    const html = fixture('minimal-recipe.html');
    const fetchFn: FetchFn = jest.fn(async () => mockResponse({ text: html }));
    const result = await fetchRecipePageHtml('https://example.com/recipe', undefined, fetchFn);
    expect(result).toBe(html);
    expect(fetchFn).toHaveBeenCalledWith(
      'https://example.com/recipe',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('throws for invalid URL', async () => {
    await expect(fetchRecipePageHtml('ftp://bad', undefined, jest.fn())).rejects.toThrow(
      'Enter a valid http or https URL.'
    );
  });

  it('throws for non-OK response', async () => {
    const fetchFn: FetchFn = jest.fn(async () => mockResponse({ ok: false, status: 404 }));
    await expect(
      fetchRecipePageHtml('https://example.com/missing', undefined, fetchFn)
    ).rejects.toThrow('Could not load page (HTTP 404).');
  });

  it('throws when page exceeds max bytes via text fallback', async () => {
    const fetchFn: FetchFn = jest.fn(async () =>
      mockResponse({ text: 'x'.repeat(NATIVE_MAX_RESPONSE_BYTES + 1) })
    );
    await expect(
      fetchRecipePageHtml('https://example.com/huge', undefined, fetchFn)
    ).rejects.toThrow('Page is too large to import.');
  });

  it('throws when streamed body exceeds max bytes', async () => {
    const chunk = new Uint8Array(NATIVE_MAX_RESPONSE_BYTES + 1);
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(chunk);
        controller.close();
      },
    });
    const fetchFn: FetchFn = jest.fn(async () => mockResponse({ body: stream }));
    await expect(
      fetchRecipePageHtml('https://example.com/huge', undefined, fetchFn)
    ).rejects.toThrow('Page is too large to import.');
  });

  it('throws Import canceled when external signal is already aborted', async () => {
    const controller = new AbortController();
    controller.abort();
    await expect(
      fetchRecipePageHtml('https://example.com/recipe', controller.signal, jest.fn())
    ).rejects.toThrow('Import canceled.');
  });

  it('throws Import canceled on timeout', async () => {
    jest.useFakeTimers();
    const fetchFn: FetchFn = jest.fn((_url, init) => {
      return new Promise((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => {
          const err = new Error('Aborted');
          err.name = 'AbortError';
          reject(err);
        });
      });
    });
    const promise = fetchRecipePageHtml('https://example.com/slow', undefined, fetchFn);
    jest.advanceTimersByTime(NATIVE_FETCH_TIMEOUT_MS + 1);
    await expect(promise).rejects.toThrow('Import canceled.');
  });
});

describe('fetchRecipePageHtml additional paths', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  it('rethrows generic network failures as fetch errors', async () => {
    const fetchFn: FetchFn = jest.fn(async () => {
      throw new Error('network down');
    });
    await expect(
      fetchRecipePageHtml('https://example.com/recipe', undefined, fetchFn)
    ).rejects.toThrow('Could not load page.');
  });

  it('decodes multi-chunk streamed bodies in order', async () => {
    const first = new TextEncoder().encode('<html>part-');
    const second = new TextEncoder().encode('two</html>');
    const stream = new ReadableStream<Uint8Array>({
      start(controller) {
        controller.enqueue(first);
        controller.enqueue(second);
        controller.close();
      },
    });
    const fetchFn: FetchFn = jest.fn(async () => mockResponse({ body: stream }));

    const result = await fetchRecipePageHtml('https://example.com/recipe', undefined, fetchFn);

    expect(result).toBe('<html>part-two</html>');
  });
});
