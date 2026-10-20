import AsyncStorage from '@react-native-async-storage/async-storage';
import * as DocumentPicker from 'expo-document-picker';
import { Directory, File as ExpoFile, Paths } from 'expo-file-system';
import { Platform } from 'react-native';

jest.mock('expo-document-picker', () => ({
  getDocumentAsync: jest.fn(),
}));

jest.mock('expo-file-system', () => ({
  Directory: { pickDirectoryAsync: jest.fn() },
  File: jest.fn(),
  Paths: { document: { uri: 'file:///Documents' } },
}));

import type { Settings } from '@/contexts/settings';
import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { emptyByDay, useWeeklyPlan } from '@/store/weekly-plan';
import {
  applyBackupImport,
  buildBackupJson,
  exportBackupToFilesystem,
  pickAndImportBackup,
} from '@/utils/backup';

const settings: Settings = { colorScheme: 'dark', portionsPerDay: 2 };

const mockGetDocumentAsync = DocumentPicker.getDocumentAsync as unknown as jest.Mock;
const mockPickDirectoryAsync = Directory.pickDirectoryAsync as unknown as jest.Mock;

const setOS = (os: string) => {
  Object.defineProperty(Platform, 'OS', { value: os, configurable: true, writable: true });
};

function backupJson() {
  useRecipes.setState({ recipes: [{ id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] }] });
  useShoppingList.setState({ items: [{ id: 'i1', label: 'Milk', checked: false }] });
  useWeeklyPlan.setState({ byDay: { ...emptyByDay(), wed: 'r1' } });
  return buildBackupJson(settings);
}

async function resetStores() {
  useRecipes.setState({ recipes: [] });
  useShoppingList.setState({ items: [] });
  useWeeklyPlan.setState({ byDay: emptyByDay() });
}

describe('pickAndImportBackup', () => {
  beforeEach(() => {
    mockGetDocumentAsync.mockReset();
    setOS('ios');
  });

  afterEach(async () => {
    await AsyncStorage.clear();
    setOS('ios');
  });

  it('returns without importing when the picker is canceled', async () => {
    mockGetDocumentAsync.mockResolvedValue({ canceled: true, assets: null });

    await pickAndImportBackup(jest.fn());

    expect(useRecipes.getState().recipes).toEqual([]);
  });

  it('imports a picked file on native via expo-file-system', async () => {
    const json = backupJson();
    await resetStores();
    (ExpoFile as unknown as jest.Mock).mockImplementation(() => ({
      text: async () => json,
    }));
    mockGetDocumentAsync.mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'file:///cache/backup.json' }],
    });

    const replaceSettings = jest.fn();
    await pickAndImportBackup(replaceSettings);

    expect(useRecipes.getState().recipes).toEqual([
      { id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] },
    ]);
    expect(replaceSettings).toHaveBeenCalledWith(settings);
  });

  it('imports a picked file on web via asset.file.text()', async () => {
    const json = backupJson();
    await resetStores();
    setOS('web');
    mockGetDocumentAsync.mockResolvedValue({
      canceled: false,
      assets: [{ uri: 'blob:x', file: { text: async () => json } }],
    });

    await pickAndImportBackup(jest.fn());

    expect(useRecipes.getState().recipes).toEqual([
      { id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] },
    ]);
  });
});

describe('exportBackupToFilesystem', () => {
  afterEach(() => {
    setOS('ios');
  });

  it('writes the backup to a picked directory on native', async () => {
    // Freeze the clock so the expected JSON and the exported payload share
    // the same exportedAt timestamp.
    jest.useFakeTimers({ now: new Date('2026-01-15T12:00:00.000Z') });
    try {
      const createFile = jest.fn((_filename: string, _mimeType: string) => ({ write: jest.fn() }));
      mockPickDirectoryAsync.mockReset();
      mockPickDirectoryAsync.mockResolvedValue({ createFile });
      setOS('ios');

      const json = backupJson();

      await exportBackupToFilesystem(settings);

      expect(mockPickDirectoryAsync).toHaveBeenCalledWith(Paths.document.uri);
      expect(createFile).toHaveBeenCalledTimes(1);
      const [filename, mimeType] = createFile.mock.calls[0];
      expect(filename).toMatch(/^backup-\d{4}-\d{2}-\d{2}\.json$/);
      expect(mimeType).toBe('application/json');
      expect(createFile.mock.results[0].value.write.mock.calls[0][0]).toBe(json);
    } finally {
      jest.useRealTimers();
    }
  });

  it('starts a browser download on web and cleans up', async () => {
    setOS('web');
    backupJson();

    const clicks: Array<{ click: jest.Mock; download: string }> = [];
    const appended: unknown[] = [];
    const removed: unknown[] = [];
    (globalThis as { document?: unknown }).document = {
      createElement: () => {
        const link = {
          click: jest.fn(),
          remove: () => removed.push(link),
        };
        clicks.push(link as never);
        return link;
      },
      body: { appendChild: (el: unknown) => appended.push(el) },
    };
    const createObjectURL = jest.fn(() => 'blob:backup');
    const revokeObjectURL = jest.fn();
    (globalThis as { URL?: unknown }).URL = { createObjectURL, revokeObjectURL };

    jest.useFakeTimers();
    try {
      await exportBackupToFilesystem(settings);

      expect(createObjectURL).toHaveBeenCalledTimes(1);
      expect(clicks[0].download).toMatch(/^backup-\d{4}-\d{2}-\d{2}\.json$/);
      expect(clicks[0].click).toHaveBeenCalledTimes(1);

      // Cleanup revokes the object URL after the download timeout.
      jest.advanceTimersByTime(300);
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:backup');
    } finally {
      jest.useRealTimers();
    }

    delete (globalThis as { document?: unknown }).document;
    delete (globalThis as { URL?: unknown }).URL;
  });

  it('rejects with a download error when blob creation fails on web', async () => {
    setOS('web');
    backupJson();

    (globalThis as { document?: unknown }).document = {
      createElement: () => ({ click: jest.fn(), remove: jest.fn() }),
      body: { appendChild: () => {} },
    };
    const createObjectURL = jest.fn(() => {
      throw new Error('no blob support');
    });
    const revokeObjectURL = jest.fn();
    (globalThis as { URL?: unknown }).URL = { createObjectURL, revokeObjectURL };

    await expect(exportBackupToFilesystem(settings)).rejects.toThrow('no blob support');
    expect(revokeObjectURL).not.toHaveBeenCalled();

    delete (globalThis as { document?: unknown }).document;
    delete (globalThis as { URL?: unknown }).URL;
  });
});
