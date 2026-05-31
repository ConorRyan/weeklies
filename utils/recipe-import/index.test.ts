import { readFileSync } from 'fs';
import { join } from 'path';

import {
  importRecipeFromUrlWithMistral,
  importRecipeWithMistral,
} from '@/utils/recipe-import/index';
import type { FetchFn } from '@/utils/recipe-import/native-fetch-url';

function fixture(name: string): string {
  return readFileSync(join(__dirname, '__fixtures__', name), 'utf-8');
}

describe('importRecipeWithMistral', () => {
  it('throws when html is empty', async () => {
    await expect(
      importRecipeWithMistral({ html: '  ', apiKey: 'sk-test' })
    ).rejects.toThrow('Page content is empty.');
  });
});

describe('importRecipeFromUrlWithMistral', () => {
  it('fetches html and returns parsed recipe', async () => {
    const html = fixture('allrecipes-with-jsonld.html');
    const mistralBody = {
      choices: [
        {
          message: {
            content: JSON.stringify({
              name: 'Tomato Pasta',
              servings: 2,
              ingredients: [{ name: 'pasta', quantity: '200 g' }],
            }),
          },
        },
      ],
    };

    const fetchFn: FetchFn = jest.fn(async (url) => {
      if (String(url).includes('mistral.ai')) {
        return {
          ok: true,
          status: 200,
          json: async () => mistralBody,
        } as Response;
      }
      return {
        ok: true,
        status: 200,
        text: async () => html,
        body: null,
      } as Response;
    }) as unknown as FetchFn;

    const originalFetch = global.fetch;
    global.fetch = fetchFn;

    try {
      const recipe = await importRecipeFromUrlWithMistral({
        url: 'https://example.com/tomato-pasta',
        apiKey: 'sk-test',
      });
      expect(recipe.name).toBe('Tomato Pasta');
      expect(recipe.ingredients[0]).toBe('100 g pasta');
      expect(fetchFn).toHaveBeenCalled();
    } finally {
      global.fetch = originalFetch;
    }
  });
});
