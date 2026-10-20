import AsyncStorage from '@react-native-async-storage/async-storage';

import { SETTINGS_STORAGE_KEY, type Settings } from '@/contexts/settings';
import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { emptyByDay, useWeeklyPlan } from '@/store/weekly-plan';
import {
  BACKUP_EXPORT_FORMAT,
  BACKUP_EXPORT_VERSION,
  applyBackupImport,
  buildBackupJson,
  defaultBackupFilename,
} from '@/utils/backup';

const settings: Settings = { colorScheme: 'dark', portionsPerDay: 2 };

const recipeA = { id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] };

const itemA = { id: 'i1', label: 'Milk', checked: false };

function seedStores() {
  useRecipes.setState({ recipes: [recipeA] });
  useShoppingList.setState({ items: [itemA] });
  useWeeklyPlan.setState({ byDay: { ...emptyByDay(), wed: 'r1' } });
}

function wipeStores() {
  useRecipes.setState({ recipes: [] });
  useShoppingList.setState({ items: [] });
  useWeeklyPlan.setState({ byDay: emptyByDay() });
}

describe('defaultBackupFilename', () => {
  afterEach(() => {
    jest.useRealTimers();
  });

  it('derives from the current date', () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(Date.UTC(2026, 9, 20, 12)));
    expect(defaultBackupFilename()).toBe('backup-2026-10-20.json');
  });

  it('zero-pads month and day', () => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(Date.UTC(2026, 0, 5, 12)));
    expect(defaultBackupFilename()).toBe('backup-2026-01-05.json');
  });
});

describe('buildBackupJson', () => {
  it('wraps the store snapshot in a valid backup payload', () => {
    seedStores();

    const payload = JSON.parse(buildBackupJson(settings));

    expect(payload.format).toBe(BACKUP_EXPORT_FORMAT);
    expect(payload.version).toBe(BACKUP_EXPORT_VERSION);
    expect(typeof payload.exportedAt).toBe('string');
    expect(payload.recipes).toEqual([recipeA]);
    expect(payload.shoppingList).toEqual({ items: [itemA] });
    expect(payload.weeklyPlan.byDay).toMatchObject({ wed: 'r1' });
    expect(payload.settings).toMatchObject({ colorScheme: 'dark', portionsPerDay: 2 });
  });
});

describe('applyBackupImport', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  it('round-trips stores through export and import', async () => {
    seedStores();
    const json = buildBackupJson(settings);
    wipeStores();

    const replaceSettings = jest.fn();
    await applyBackupImport(json, replaceSettings);

    expect(useRecipes.getState().recipes).toEqual([recipeA]);
    expect(useShoppingList.getState().items).toEqual([itemA]);
    expect(useWeeklyPlan.getState().byDay.wed).toBe('r1');
    expect(replaceSettings).toHaveBeenCalledWith({ colorScheme: 'dark', portionsPerDay: 2 });
  });

  it('persists restored settings to storage', async () => {
    seedStores();
    const json = buildBackupJson(settings);
    await AsyncStorage.clear();

    await applyBackupImport(json, jest.fn());

    const raw = await AsyncStorage.getItem(SETTINGS_STORAGE_KEY);
    expect(raw).toContain('"colorScheme":"dark"');
    expect(raw).toContain('"portionsPerDay":2');
  });

  it('rejects text that is not JSON', async () => {
    await expect(applyBackupImport('not json at all', jest.fn())).rejects.toThrow(
      'Could not parse JSON'
    );
  });

  it('rejects files that are not backups', async () => {
    await expect(
      applyBackupImport(JSON.stringify({ format: 'nope', version: 1 }), jest.fn())
    ).rejects.toThrow('Not a valid backup file');
  });
});
