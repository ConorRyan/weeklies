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

describe('parseRecipeHtml field handling', () => {
  function htmlWithJsonLd(...nodes: unknown[]): string {
    const blocks = nodes
      .map((n) => `<script type="application/ld+json">${JSON.stringify(n)}</script>`)
      .join('');
    return `<html><head>${blocks}</head></html>`;
  }

  it('combines value and name for ingredient objects with amounts', () => {
    const html = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'Bread',
      recipeIngredient: [{ name: 'flour', value: '500 g' }, { name: 'salt' }],
    });

    expect(parseRecipeHtml(html)).toEqual({
      url: undefined,
      name: 'Bread',
      recipeYield: undefined,
      recipeIngredients: ['500 g flour', 'salt'],
    });
  });

  it('cleans unicode fractions and inline tags from ingredient lines', () => {
    const html = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'Cake',
      recipeIngredient: ['½ cup <b>sugar</b>', '¾ tsp  vanilla'],
    });

    expect(parseRecipeHtml(html).recipeIngredients).toEqual([
      '1/2 cup sugar',
      '3/4 tsp vanilla',
    ]);
  });

  it('accepts array or alternate yield fields', () => {
    const withArray = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'A',
      recipeIngredient: ['x'],
      recipeYield: ['4 servings', '2 large'],
    });
    const withYield = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'B',
      recipeIngredient: ['x'],
      yield: '2 portions',
    });

    expect(parseRecipeHtml(withArray).recipeYield).toBe('4 servings');
    expect(parseRecipeHtml(withYield).recipeYield).toBe('2 portions');
  });

  it('falls back to ingredients/ingredient property names', () => {
    const viaIngredients = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'A',
      ingredients: ['1 onion'],
    });
    const viaIngredient = htmlWithJsonLd({
      '@type': 'Recipe',
      name: 'B',
      ingredient: ['1 carrot'],
    });

    expect(parseRecipeHtml(viaIngredients).recipeIngredients).toEqual(['1 onion']);
    expect(parseRecipeHtml(viaIngredient).recipeIngredients).toEqual(['1 carrot']);
  });

  it('skips incomplete recipe nodes and uses the first complete one', () => {
    const html = htmlWithJsonLd(
      { '@type': 'Recipe', name: 'No ingredients' },
      { '@type': 'Recipe', name: 'Good', recipeIngredient: ['1 egg'] },
      { '@type': 'Recipe', name: 'Also good', recipeIngredient: ['2 eggs'] }
    );

    expect(parseRecipeHtml(html).name).toBe('Good');
  });

  it('trims the url option before attaching it', () => {
    const html = htmlWithJsonLd({ '@type': 'Recipe', name: 'A', recipeIngredient: ['x'] });

    expect(parseRecipeHtml(html, { url: '  https://example.com/a  ' }).url).toBe(
      'https://example.com/a'
    );
  });
});
