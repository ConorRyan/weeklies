import type { Settings } from '@/contexts/settings';
import { serializeSettingsStorage } from '@/contexts/settings';
import { normalizeRecipe, type Recipe } from '@/store/recipes';
import { normalizeItem, type Item } from '@/store/shopping-list';
import { WEEKDAYS, emptyByDay, type DayAssignments } from '@/store/weekly-plan';

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

export type BackupSnapshot = {
  byDay: DayAssignments;
  recipes: Recipe[];
  items: Item[];
};

export function normalizeWeeklyPlanImport(raw: unknown): DayAssignments {
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

export function sanitizeRecipesForImport(raw: unknown): Recipe[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map(normalizeRecipe).filter((r): r is Recipe => r !== null);
}

export function sanitizeItemsForImport(raw: unknown): Item[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map(normalizeItem).filter((i): i is Item => i !== null);
}

export function validateBackupPayload(data: unknown): asserts data is Record<string, unknown> {
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

export function buildBackupPayload(
  settings: Settings,
  snapshot: BackupSnapshot,
  exportedAt: string
): BackupPayload {
  return {
    format: BACKUP_EXPORT_FORMAT,
    version: BACKUP_EXPORT_VERSION,
    exportedAt,
    settings: JSON.parse(serializeSettingsStorage(settings)) as BackupPayload['settings'],
    weeklyPlan: { byDay: snapshot.byDay },
    recipes: snapshot.recipes,
    shoppingList: { items: snapshot.items },
  };
}
