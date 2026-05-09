import AsyncStorage from '@react-native-async-storage/async-storage';
import * as DocumentPicker from 'expo-document-picker';
import { Directory, File as ExpoFile, Paths } from 'expo-file-system';

import {
  SETTINGS_STORAGE_KEY,
  parseStoredSettings,
  serializeSettingsStorage,
  type Settings,
} from '@/contexts/settings';
import {
  RECIPES_STORAGE_KEY,
  SHOPPING_LIST_STORAGE_KEY,
  WEEKLY_PLAN_STORAGE_KEY,
  flushPendingShoppingListWrites,
} from '@/store/persistence';
import { normalizeRecipe, useRecipes, type Recipe } from '@/store/recipes';
import { normalizeItem, useShoppingList, type Item } from '@/store/shopping-list';
import { WEEKDAYS, emptyByDay, useWeeklyPlan, type DayAssignments } from '@/store/weekly-plan';

export const BACKUP_EXPORT_FORMAT = 'backup' as const;

export const BACKUP_EXPORT_VERSION = 1 as const;

export type BackupPayload = {
  format: typeof BACKUP_EXPORT_FORMAT;
  version: typeof BACKUP_EXPORT_VERSION;
  exportedAt: string;
  settings: unknown;
  weeklyPlan: { byDay: DayAssignments };
  recipes: Recipe[];
  shoppingList: { items: Item[] };
};

export function defaultBackupFilename(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `backup-${y}-${m}-${day}.json`;
}

function normalizeWeeklyPlanImport(raw: unknown): DayAssignments {
  const next = emptyByDay();
  if (!raw || typeof raw !== 'object') {
    return next;
  }
  const byDay = (raw as { byDay?: unknown }).byDay;
  if (!byDay || typeof byDay !== 'object') {
    return next;
  }
  for (const day of WEEKDAYS) {
    const v = (byDay as Record<string, unknown>)[day];
    next[day] = v === null || v === undefined ? null : typeof v === 'string' ? v : null;
  }
  return next;
}

function sanitizeRecipes(raw: unknown): Recipe[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map(normalizeRecipe).filter((r): r is Recipe => r !== null);
}

function sanitizeItems(raw: unknown): Item[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map(normalizeItem).filter((i): i is Item => i !== null);
}

function validateBackupPayload(data: unknown): asserts data is Record<string, unknown> {
  if (!data || typeof data !== 'object') {
    throw new Error('Invalid file');
  }
  const o = data as Record<string, unknown>;
  if (o.format !== BACKUP_EXPORT_FORMAT) {
    throw new Error('Not a valid backup file');
  }
  if (o.version !== BACKUP_EXPORT_VERSION) {
    throw new Error('Unsupported backup version');
  }
}

export function buildBackupJson(settings: Settings): string {
  const exportedAt = new Date().toISOString();
  const payload: BackupPayload = {
    format: BACKUP_EXPORT_FORMAT,
    version: BACKUP_EXPORT_VERSION,
    exportedAt,
    settings: JSON.parse(serializeSettingsStorage(settings)) as BackupPayload['settings'],
    weeklyPlan: { byDay: useWeeklyPlan.getState().byDay },
    recipes: useRecipes.getState().recipes,
    shoppingList: { items: useShoppingList.getState().items },
  };
  return JSON.stringify(payload, null, 2);
}

export async function applyBackupImport(
  rawJson: string,
  replaceSettings: (next: Settings) => void
): Promise<void> {
  let data: unknown;
  try {
    data = JSON.parse(rawJson) as unknown;
  } catch {
    throw new Error('Could not parse JSON');
  }

  validateBackupPayload(data);
  const weeklyByDay = normalizeWeeklyPlanImport(data.weeklyPlan);
  const recipes = sanitizeRecipes(data.recipes);
  const items = sanitizeItems(
    data.shoppingList && typeof data.shoppingList === 'object'
      ? (data.shoppingList as { items?: unknown }).items
      : undefined
  );
  const settings = parseStoredSettings(JSON.stringify(data.settings ?? {}));

  await flushPendingShoppingListWrites();

  await AsyncStorage.multiSet([
    [SETTINGS_STORAGE_KEY, serializeSettingsStorage(settings)],
    [RECIPES_STORAGE_KEY, JSON.stringify({ state: { recipes }, version: 2 })],
    [SHOPPING_LIST_STORAGE_KEY, JSON.stringify({ state: { items }, version: 2 })],
    [WEEKLY_PLAN_STORAGE_KEY, JSON.stringify({ state: { byDay: weeklyByDay }, version: 1 })],
  ]);

  await Promise.all([
    useRecipes.persist.rehydrate(),
    useShoppingList.persist.rehydrate(),
    useWeeklyPlan.persist.rehydrate(),
  ]);

  replaceSettings(settings);
}

async function readPickedAssetText(asset: DocumentPicker.DocumentPickerAsset): Promise<string> {
  const f = new ExpoFile(asset.uri);
  return f.text();
}

export async function pickAndImportBackup(replaceSettings: (next: Settings) => void): Promise<void> {
  const result = await DocumentPicker.getDocumentAsync({
    type: ['application/json', 'text/plain'],
    copyToCacheDirectory: true,
    base64: false,
  });

  if (result.canceled || !result.assets?.length) {
    return;
  }

  const text = await readPickedAssetText(result.assets[0]);
  await applyBackupImport(text, replaceSettings);
}

export async function exportBackupToFilesystem(settings: Settings): Promise<void> {
  const json = buildBackupJson(settings);
  const filename = defaultBackupFilename();

  const pickedDir = await Directory.pickDirectoryAsync(Paths.document.uri);
  // Use `createFile` on the picked directory so SAF/content providers get a proper document URI (joining
  // `pickedDir.uri` + filename as strings breaks on Android and can mis-create paths).
  const file = pickedDir.createFile(filename, 'application/json');
  file.write(json);
}
