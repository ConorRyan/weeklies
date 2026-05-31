import { parseRecipeYield, scrapedRecipeToNewRecipe } from '@/utils/recipe-import/scraper-to-recipe';

describe('parseRecipeYield', () => {
  it('parses a plain number', () => {
    expect(parseRecipeYield('4')).toBe(4);
  });

  it('parses "4 servings"', () => {
    expect(parseRecipeYield('4 servings')).toBe(4);
  });

  it('parses "Serves 6"', () => {
    expect(parseRecipeYield('Serves 6')).toBe(6);
  });

  it('uses the first number in a range', () => {
    expect(parseRecipeYield('6-8 servings')).toBe(6);
  });

  it('returns null for empty or unparseable values', () => {
    expect(parseRecipeYield('')).toBeNull();
    expect(parseRecipeYield(undefined)).toBeNull();
    expect(parseRecipeYield('no yield')).toBeNull();
    expect(parseRecipeYield('0')).toBeNull();
  });
});

describe('scrapedRecipeToNewRecipe', () => {
  it('scales ingredients from recipe yield to one portion', () => {
    const result = scrapedRecipeToNewRecipe({
      name: 'Pasta',
      recipeYield: '4 servings',
      recipeIngredients: ['2 cups flour', '4 eggs'],
    });
    expect(result).toEqual({
      name: 'Pasta',
      ingredients: ['0.5 cups flour', '1 eggs'],
    });
  });

  it('returns ingredients unchanged when yield is missing', () => {
    const result = scrapedRecipeToNewRecipe({
      name: 'Pasta',
      recipeIngredients: ['2 cups flour'],
    });
    expect(result).toEqual({
      name: 'Pasta',
      ingredients: ['2 cups flour'],
    });
  });

  it('returns null when name is missing', () => {
    expect(
      scrapedRecipeToNewRecipe({
        recipeIngredients: ['2 cups flour'],
      })
    ).toBeNull();
  });

  it('returns null when ingredients are empty', () => {
    expect(
      scrapedRecipeToNewRecipe({
        name: 'Pasta',
        recipeIngredients: [],
      })
    ).toBeNull();
  });
});
