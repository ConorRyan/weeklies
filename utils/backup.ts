import AsyncStorage from '@react-native-async-storage/async-storage';
import * as DocumentPicker from 'expo-document-picker';
import { Directory, File as ExpoFile, Paths } from 'expo-file-system';
import { Platform } from 'react-native';

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
import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { useWeeklyPlan } from '@/store/weekly-plan';
import {
  BACKUP_EXPORT_FORMAT,
  BACKUP_EXPORT_VERSION,
  buildBackupPayload,
  normalizeWeeklyPlanImport,
  sanitizeItemsForImport,
  sanitizeRecipesForImport,
  validateBackupPayload,
  type BackupPayload,
} from '@/utils/backup-payload';

export {
  BACKUP_EXPORT_FORMAT,
  BACKUP_EXPORT_VERSION,
  type BackupPayload,
} from '@/utils/backup-payload';

export function defaultBackupFilename(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `backup-${y}-${m}-${day}.json`;
}

export function buildBackupJson(settings: Settings): string {
  const payload = buildBackupPayload(
    settings,
    {
      byDay: useWeeklyPlan.getState().byDay,
      recipes: useRecipes.getState().recipes,
      items: useShoppingList.getState().items,
    },
    new Date().toISOString()
  );
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
  const recipes = sanitizeRecipesForImport(data.recipes);
  const items = sanitizeItemsForImport(
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
  if (Platform.OS === 'web' && asset.file) {
    return asset.file.text();
  }
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

  if (Platform.OS === 'web') {
    if (typeof document === 'undefined') {
      return;
    }
    let objectUrl: string | null = null;
    let link: HTMLAnchorElement | null = null;
    try {
      const blob = new Blob([json], { type: 'application/json' });
      objectUrl = URL.createObjectURL(blob);
      link = document.createElement('a');
      link.href = objectUrl;
      link.download = filename;
      link.rel = 'noopener noreferrer';
      document.body.appendChild(link);
      link.click();
    } catch (e) {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
      throw e instanceof Error ? e : new Error('Could not start download.');
    }
    const cleanup = (): void => {
      try {
        link?.remove();
      } catch {
        // Navigating away can detach nodes; avoid surfacing DOM errors.
      }
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
      }
    };
    setTimeout(cleanup, 250);
    return;
  }

  const pickedDir = await Directory.pickDirectoryAsync(Paths.document.uri);
  const file = pickedDir.createFile(filename, 'application/json');
  file.write(json);
}
