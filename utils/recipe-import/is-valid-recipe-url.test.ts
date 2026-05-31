import { isValidRecipeUrl } from '@/utils/recipe-import/native-fetch-url';

describe('isValidRecipeUrl', () => {
  it('accepts http and https URLs', () => {
    expect(isValidRecipeUrl('https://example.com/recipe')).toBe(true);
    expect(isValidRecipeUrl('http://example.com/recipe')).toBe(true);
  });

  it('rejects empty and whitespace-only strings', () => {
    expect(isValidRecipeUrl('')).toBe(false);
    expect(isValidRecipeUrl('   ')).toBe(false);
  });

  it('rejects non-http protocols', () => {
    expect(isValidRecipeUrl('ftp://example.com/recipe')).toBe(false);
  });

  it('rejects invalid URLs', () => {
    expect(isValidRecipeUrl('not a url')).toBe(false);
  });

  it('trims surrounding whitespace', () => {
    expect(isValidRecipeUrl('  https://example.com  ')).toBe(true);
  });
});
