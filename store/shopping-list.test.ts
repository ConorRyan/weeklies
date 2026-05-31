import { normalizeItem } from '@/store/shopping-list';

describe('normalizeItem', () => {
  it('normalizes item with label', () => {
    expect(
      normalizeItem({ id: '1', label: 'Milk', checked: false })
    ).toEqual({ id: '1', label: 'Milk', checked: false });
  });

  it('converts legacy name/quantity to label', () => {
    expect(
      normalizeItem({ id: '1', name: 'milk', quantity: '1 L', checked: true })
    ).toEqual({ id: '1', label: '1 L milk', checked: true });
  });

  it('uses name alone when quantity empty', () => {
    expect(
      normalizeItem({ id: '1', name: 'milk', quantity: '', checked: false })
    ).toEqual({ id: '1', label: 'milk', checked: false });
  });

  it('returns null for invalid shapes', () => {
    expect(normalizeItem(null)).toBeNull();
    expect(normalizeItem({ id: '1', name: '  ', quantity: '  ', checked: false })).toBeNull();
    expect(normalizeItem({ id: 1, label: 'x', checked: false })).toBeNull();
  });
});
