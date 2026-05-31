import { readFileSync } from 'fs';
import { join } from 'path';

import {
  importRecipeFromHtmlWithScraper,
  importRecipeFromUrlWithScraper,
} from '@/utils/recipe-import/scraper-import';
import type { FetchFn } from '@/utils/recipe-import/native-fetch-url';

function fixture(name: string): string {
  return readFileSync(join(__dirname, '__fixtures__', name), 'utf-8');
}

describe('importRecipeFromHtmlWithScraper', () => {
  it('throws when html is empty', async () => {
    await expect(importRecipeFromHtmlWithScraper({ html: '  ' })).rejects.toThrow(
      'Paste or load page content before importing.'
    );
  });

  it('parses JSON-LD fixture and scales to one portion', async () => {
    const html = fixture('allrecipes-with-jsonld.html');
    const recipe = await importRecipeFromHtmlWithScraper({ html });
    expect(recipe.name).toBe('Tomato Pasta');
    expect(recipe.ingredients).toEqual(['200 g pasta', '1 tomatoes']);
  });

  it('throws when no recipe data is found', async () => {
    const html = fixture('minimal-recipe.html');
    await expect(importRecipeFromHtmlWithScraper({ html })).rejects.toThrow(
      "This page doesn't include recipe data we can read."
    );
  });
});

describe('importRecipeFromUrlWithScraper', () => {
  it('fetches html and returns parsed recipe', async () => {
    const html = fixture('allrecipes-with-jsonld.html');

    const fetchFn: FetchFn = jest.fn(async () => {
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
      const recipe = await importRecipeFromUrlWithScraper({
        url: 'https://example.com/tomato-pasta',
      });
      expect(recipe.name).toBe('Tomato Pasta');
      expect(recipe.ingredients[0]).toBe('200 g pasta');
      expect(fetchFn).toHaveBeenCalled();
    } finally {
      global.fetch = originalFetch;
    }
  });
});
