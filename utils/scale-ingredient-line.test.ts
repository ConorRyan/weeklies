import { scaleIngredientLine } from '@/utils/scale-ingredient-line';

describe('scaleIngredientLine', () => {
  it('returns line unchanged when factor is 1', () => {
    expect(scaleIngredientLine('2 cups flour', 1)).toBe('2 cups flour');
  });

  it('returns line unchanged when factor is not finite', () => {
    expect(scaleIngredientLine('2 cups flour', NaN)).toBe('2 cups flour');
  });

  it('scales integers', () => {
    expect(scaleIngredientLine('2 cups flour', 2)).toBe('4 cups flour');
  });

  it('scales decimals', () => {
    expect(scaleIngredientLine('1.5 tsp salt', 2)).toBe('3 tsp salt');
  });

  it('scales fractions without spaces', () => {
    expect(scaleIngredientLine('1/2 cup sugar', 2)).toBe('1 cup sugar');
  });

  it('scales fractions with spaces', () => {
    expect(scaleIngredientLine('1 / 2 cup sugar', 2)).toBe('1 cup sugar');
  });

  it('leaves non-numeric tokens untouched', () => {
    expect(scaleIngredientLine('salt to taste', 2)).toBe('salt to taste');
  });

  it('scales multiple numeric tokens', () => {
    expect(scaleIngredientLine('1 1/2 cups flour', 2)).toBe('2 1 cups flour');
  });
});

describe('scaleIngredientLine edge cases', () => {
  it('leaves zero-denominator fractions untouched', () => {
    expect(scaleIngredientLine('1/0 cups', 2)).toBe('1/0 cups');
  });

  it('renders overflowing values as Infinity', () => {
    expect(scaleIngredientLine('9999999999999999999999999999 g', 1e308)).toBe('Infinity g');
  });

  it('rounds repeating decimals to four places', () => {
    expect(scaleIngredientLine('1 cup', 1 / 3)).toBe('0.3333 cup');
  });
});
