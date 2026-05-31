import { readFileSync } from 'fs';
import { join } from 'path';

import { recipeImportErrors } from '@/utils/recipe-import/import-errors';
import { parseRecipeHtml } from '@/utils/recipe-import/scraper-parse';

function fixture(name: string): string {
  return readFileSync(join(__dirname, '__fixtures__', name), 'utf-8');
}

describe('parseRecipeHtml', () => {
  it('parses JSON-LD recipe from fixture', () => {
    const html = fixture('allrecipes-with-jsonld.html');
    const scraped = parseRecipeHtml(html, { url: 'https://example.com/pasta' });

    expect(scraped).toEqual({
      url: 'https://example.com/pasta',
      name: 'Tomato Pasta',
      recipeYield: '2 servings',
      recipeIngredients: ['400 g pasta', '2 tomatoes'],
    });
  });

  it('throws when page has no schema data', () => {
    const html = fixture('minimal-recipe.html');
    expect(() => parseRecipeHtml(html)).toThrow(recipeImportErrors.noRecipeData());
  });
});
