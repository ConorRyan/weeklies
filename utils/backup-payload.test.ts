import {
  BACKUP_EXPORT_FORMAT,
  BACKUP_EXPORT_VERSION,
  buildBackupPayload,
  normalizeWeeklyPlanImport,
  sanitizeItemsForImport,
  sanitizeRecipesForImport,
  validateBackupPayload,
} from '@/utils/backup-payload';

describe('validateBackupPayload', () => {
  it('accepts valid backup payload', () => {
    expect(() =>
      validateBackupPayload({ format: BACKUP_EXPORT_FORMAT, version: BACKUP_EXPORT_VERSION })
    ).not.toThrow();
  });

  it('rejects non-object', () => {
    expect(() => validateBackupPayload(null)).toThrow('Invalid file');
  });

  it('rejects wrong format', () => {
    expect(() =>
      validateBackupPayload({ format: 'other', version: BACKUP_EXPORT_VERSION })
    ).toThrow('Not a valid backup file');
  });

  it('rejects unsupported version', () => {
    expect(() =>
      validateBackupPayload({ format: BACKUP_EXPORT_FORMAT, version: 99 })
    ).toThrow('Unsupported backup version');
  });
});

describe('normalizeWeeklyPlanImport', () => {
  it('returns empty plan for invalid input', () => {
    const result = normalizeWeeklyPlanImport(null);
    expect(result.mon).toBeNull();
    expect(result.sun).toBeNull();
  });

  it('keeps valid recipe ids and nulls invalid values', () => {
    const result = normalizeWeeklyPlanImport({
      byDay: { mon: 'recipe-1', tue: 42, wed: null },
    });
    expect(result.mon).toBe('recipe-1');
    expect(result.tue).toBeNull();
    expect(result.wed).toBeNull();
  });
});

describe('sanitizeRecipesForImport', () => {
  it('normalizes valid and legacy recipes', () => {
    const result = sanitizeRecipesForImport([
      { id: '1', name: 'Pasta', ingredients: ['salt'] },
      { id: '2', name: 'Soup', ingredients: [{ quantity: '1 L', name: 'water' }] },
      null,
    ]);
    expect(result).toHaveLength(2);
    expect(result[1].ingredients[0]).toBe('1 L water');
  });

  it('returns empty array for non-array input', () => {
    expect(sanitizeRecipesForImport(undefined)).toEqual([]);
  });
});

describe('sanitizeItemsForImport', () => {
  it('normalizes valid and legacy items', () => {
    const result = sanitizeItemsForImport([
      { id: '1', label: 'Milk', checked: false },
      { id: '2', name: 'eggs', quantity: '6', checked: true },
    ]);
    expect(result).toHaveLength(2);
    expect(result[1].label).toBe('6 eggs');
  });
});

describe('buildBackupPayload', () => {
  it('assembles payload from settings and snapshot', () => {
    const payload = buildBackupPayload(
      { colorScheme: 'dark', portionsPerDay: 2 },
      {
        byDay: { mon: 'r1', tue: null, wed: null, thu: null, fri: null, sat: null, sun: null },
        recipes: [{ id: 'r1', name: 'Pasta', ingredients: ['salt'] }],
        items: [{ id: 'i1', label: 'Milk', checked: false }],
      },
      '2024-01-01T00:00:00.000Z'
    );
    expect(payload.format).toBe(BACKUP_EXPORT_FORMAT);
    expect(payload.version).toBe(BACKUP_EXPORT_VERSION);
    expect(payload.exportedAt).toBe('2024-01-01T00:00:00.000Z');
    expect(payload.recipes).toHaveLength(1);
    expect(payload.shoppingList.items).toHaveLength(1);
    expect(payload.weeklyPlan.byDay.mon).toBe('r1');
  });
});
