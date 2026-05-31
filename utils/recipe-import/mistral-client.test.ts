import { readFileSync } from 'fs';
import { join } from 'path';

import { mapMistralHttpError, extractRecipeWithMistral } from '@/utils/recipe-import/mistral-client';
import { preprocessRecipeHtml } from '@/utils/recipe-import/preprocess';
import type { FetchFn } from '@/utils/recipe-import/native-fetch-url';

function fixture(name: string): string {
  return readFileSync(join(__dirname, '__fixtures__', name), 'utf-8');
}

describe('mapMistralHttpError', () => {
  it('maps known status codes', () => {
    expect(mapMistralHttpError(401)).toContain('Invalid Mistral API key');
    expect(mapMistralHttpError(429)).toContain('rate limit');
    expect(mapMistralHttpError(503)).toContain('temporarily unavailable');
  });

  it('maps unknown status codes', () => {
    expect(mapMistralHttpError(418)).toBe('Mistral request failed (418).');
  });
});

describe('extractRecipeWithMistral', () => {
  const preprocessed = preprocessRecipeHtml(fixture('minimal-recipe.html'));

  it('throws when api key is empty', async () => {
    await expect(
      extractRecipeWithMistral({ apiKey: '  ', preprocessed }, jest.fn())
    ).rejects.toThrow('Add your Mistral API key');
  });

  it('returns parsed recipe on happy path', async () => {
    const body = JSON.parse(fixture('mistral-response.json')) as {
      choices: { message: { content: string } }[];
    };
    const fetchFn: FetchFn = jest.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => body,
    })) as unknown as FetchFn;

    const recipe = await extractRecipeWithMistral(
      { apiKey: 'sk-test', preprocessed },
      fetchFn
    );
    expect(recipe.name).toBe('Tomato Pasta');
    expect(recipe.ingredients).toEqual(['100 g pasta', '0.5 tomatoes']);
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('throws mapped error for HTTP failures', async () => {
    const fetchFn: FetchFn = jest.fn(async () => ({
      ok: false,
      status: 401,
      json: async () => ({}),
    })) as unknown as FetchFn;

    await expect(
      extractRecipeWithMistral({ apiKey: 'sk-test', preprocessed }, fetchFn)
    ).rejects.toThrow('Invalid Mistral API key');
  });

  it('throws on empty assistant content', async () => {
    const fetchFn: FetchFn = jest.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({ choices: [{ message: { content: '  ' } }] }),
    })) as unknown as FetchFn;

    await expect(
      extractRecipeWithMistral({ apiKey: 'sk-test', preprocessed }, fetchFn)
    ).rejects.toThrow('empty response');
  });

  it('retries once when first response JSON is invalid', async () => {
    const goodContent = JSON.stringify({
      name: 'Soup',
      servings: 2,
      ingredients: [{ name: 'water', quantity: '2 cups' }],
    });
    const fetchFn: FetchFn = jest
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          choices: [{ message: { content: fixture('mistral-bad-json.txt') } }],
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          choices: [{ message: { content: goodContent } }],
        }),
      }) as unknown as FetchFn;

    const recipe = await extractRecipeWithMistral(
      { apiKey: 'sk-test', preprocessed },
      fetchFn
    );
    expect(recipe.name).toBe('Soup');
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });
});
