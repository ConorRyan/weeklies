import {
  ingredientPartsToLine,
  isValidServings,
  mistralPayloadToNewRecipe,
  parseMistralContentJson,
  parseMistralRecipePayload,
  scaleIngredientsToServings,
} from '@/utils/recipe-import/parse-recipe';

describe('isValidServings', () => {
  it('accepts positive finite numbers', () => {
    expect(isValidServings(4)).toBe(true);
  });

  it('rejects zero and negative', () => {
    expect(isValidServings(0)).toBe(false);
    expect(isValidServings(-1)).toBe(false);
  });

  it('rejects non-finite', () => {
    expect(isValidServings(NaN)).toBe(false);
  });
});

describe('scaleIngredientsToServings', () => {
  it('scales ingredients from 4 servings to 1', () => {
    const result = scaleIngredientsToServings(['2 cups flour'], 4, 1);
    expect(result[0]).toBe('0.5 cups flour');
  });

  it('returns unchanged when servings invalid', () => {
    const ingredients = ['2 cups flour'];
    expect(scaleIngredientsToServings(ingredients, 0, 1)).toBe(ingredients);
  });
});

describe('ingredientPartsToLine', () => {
  it('joins quantity and name', () => {
    expect(ingredientPartsToLine('200 g', 'flour')).toBe('200 g flour');
  });

  it('returns null for empty name', () => {
    expect(ingredientPartsToLine('200 g', '  ')).toBeNull();
  });

  it('handles missing quantity', () => {
    expect(ingredientPartsToLine(undefined, 'salt')).toBe('salt');
  });
});

describe('parseMistralRecipePayload', () => {
  it('parses valid payload', () => {
    const result = parseMistralRecipePayload({
      name: 'Pasta',
      servings: 4,
      ingredients: [{ name: 'pasta', quantity: '200 g' }],
    });
    expect(result).toEqual({
      name: 'Pasta',
      servings: 4,
      ingredients: [{ name: 'pasta', quantity: '200 g' }],
    });
  });

  it('parses string servings', () => {
    const result = parseMistralRecipePayload({
      name: 'Pasta',
      servings: '4',
      ingredients: [{ name: 'pasta' }],
    });
    expect(result?.servings).toBe(4);
  });

  it('returns null for invalid payload', () => {
    expect(parseMistralRecipePayload(null)).toBeNull();
    expect(parseMistralRecipePayload({ name: 'X', servings: 0, ingredients: [] })).toBeNull();
    expect(
      parseMistralRecipePayload({ name: 'X', servings: 2, ingredients: [{ noName: true }] })
    ).toBeNull();
  });
});

describe('mistralPayloadToNewRecipe', () => {
  it('builds recipe scaled to target servings', () => {
    const result = mistralPayloadToNewRecipe(
      {
        name: 'Pasta',
        servings: 4,
        ingredients: [{ name: 'pasta', quantity: '400 g' }],
      },
      1
    );
    expect(result).toEqual({
      name: 'Pasta',
      ingredients: ['100 g pasta'],
    });
  });

  it('returns null for empty name', () => {
    expect(
      mistralPayloadToNewRecipe(
        { name: '  ', servings: 2, ingredients: [{ name: 'salt' }] },
        1
      )
    ).toBeNull();
  });

  it('returns null when no valid ingredients', () => {
    expect(
      mistralPayloadToNewRecipe(
        { name: 'Pasta', servings: 2, ingredients: [{ name: '  ' }] },
        1
      )
    ).toBeNull();
  });
});

describe('parseMistralContentJson', () => {
  it('parses JSON string content', () => {
    const content = JSON.stringify({
      name: 'Soup',
      servings: 2,
      ingredients: [{ name: 'water' }],
    });
    expect(parseMistralContentJson(content)?.name).toBe('Soup');
  });

  it('returns null for malformed JSON', () => {
    expect(parseMistralContentJson('{bad')).toBeNull();
  });
});
