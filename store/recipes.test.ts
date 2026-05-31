import { normalizeRecipe } from '@/store/recipes';

describe('normalizeRecipe', () => {
  it('normalizes valid recipe with string ingredients', () => {
    expect(
      normalizeRecipe({
        id: '1',
        name: 'Pasta',
        ingredients: ['200 g pasta', 'salt'],
      })
    ).toEqual({
      id: '1',
      name: 'Pasta',
      ingredients: ['200 g pasta', 'salt'],
    });
  });

  it('converts legacy ingredient objects', () => {
    expect(
      normalizeRecipe({
        id: '1',
        name: 'Pasta',
        ingredients: [{ quantity: '200 g', name: 'pasta' }],
      })
    ).toEqual({
      id: '1',
      name: 'Pasta',
      ingredients: ['200 g pasta'],
    });
  });

  it('filters empty ingredient strings', () => {
    expect(
      normalizeRecipe({
        id: '1',
        name: 'Pasta',
        ingredients: ['salt', '  ', ''],
      })
    ).toEqual({
      id: '1',
      name: 'Pasta',
      ingredients: ['salt'],
    });
  });

  it('returns null for invalid shapes', () => {
    expect(normalizeRecipe(null)).toBeNull();
    expect(normalizeRecipe({ id: 1, name: 'X', ingredients: [] })).toBeNull();
    expect(normalizeRecipe({ id: '1', name: 'X' })).toBeNull();
  });
});
