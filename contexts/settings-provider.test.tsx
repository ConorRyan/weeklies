import { act, renderHook, waitFor } from '@testing-library/react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { ReactNode } from 'react';

import {
  SETTINGS_STORAGE_KEY,
  SettingsProvider,
  parseStoredSettings,
  serializeSettingsStorage,
  usePortionsPerDay,
  useSettings,
  useThemePreference,
} from '@/contexts/settings';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

function useAllSettingsHooks() {
  return {
    settings: useSettings().settings,
    patchSettings: useSettings().patchSettings,
    replaceSettings: useSettings().replaceSettings,
    theme: useThemePreference(),
    portions: usePortionsPerDay(),
  };
}

describe('SettingsProvider', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  it('hydrates settings persisted in storage', async () => {
    await AsyncStorage.setItem(
      SETTINGS_STORAGE_KEY,
      serializeSettingsStorage({ colorScheme: 'light', portionsPerDay: 4 })
    );

    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await waitFor(() => expect(result.current.settings.portionsPerDay).toBe(4));
    expect(result.current.settings.colorScheme).toBe('light');
  });

  it('persists patched settings', async () => {
    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.patchSettings({ colorScheme: 'light', portionsPerDay: 3 });
    });

    const restored = parseStoredSettings(
      await AsyncStorage.getItem(SETTINGS_STORAGE_KEY)
    );
    expect(restored).toEqual({ colorScheme: 'light', portionsPerDay: 3 });
  });

  it('ignores invalid patch values', async () => {
    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.patchSettings({ colorScheme: 'purple' as never, portionsPerDay: 0 });
    });

    expect(result.current.settings.portionsPerDay).toBe(2);
    expect(result.current.settings.colorScheme).toBe('dark');
  });

  it('replaceSettings falls back to defaults for invalid values', async () => {
    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.replaceSettings({ colorScheme: 'purple' as never, portionsPerDay: -1 });
    });

    expect(result.current.settings.portionsPerDay).toBe(2);
  });

  it('setPortionsPerDay ignores invalid numbers', async () => {
    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.portions.setPortionsPerDay(0);
    });

    expect(result.current.settings.portionsPerDay).toBe(2);

    await act(async () => {
      result.current.portions.setPortionsPerDay(5);
    });

    expect(result.current.settings.portionsPerDay).toBe(5);
  });

  it('setColorScheme flows through the theme preference', async () => {
    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.theme.setColorScheme('light');
    });

    expect(result.current.theme.colorScheme).toBe('light');
    expect(result.current.settings.colorScheme).toBe('light');
  });

  it('keeps the user mutation when hydration lands afterwards', async () => {
    let resolveRead: ((raw: string | null) => void) | undefined;
    const getItemSpy = jest.spyOn(AsyncStorage, 'getItem').mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRead = resolve;
        })
    );

    const { result } = await renderHook(() => useAllSettingsHooks(), { wrapper });

    await act(async () => {
      result.current.patchSettings({ portionsPerDay: 6 });
    });

    // Hydration finishes after the user already changed settings.
    await act(async () => {
      resolveRead?.(serializeSettingsStorage({ colorScheme: 'light', portionsPerDay: 4 }));
    });

    expect(result.current.settings.portionsPerDay).toBe(6);
    expect(result.current.settings.colorScheme).toBe('dark');
    getItemSpy.mockRestore();
  });
});

describe('settings hooks without a provider', () => {
  it('throw for useSettings', async () => {
    await expect(renderHook(() => useSettings())).rejects.toThrow(
      'useSettings must be used within SettingsProvider'
    );
  });

  it('throw for useThemePreference', async () => {
    await expect(renderHook(() => useThemePreference())).rejects.toThrow(
      'useThemePreference must be used within SettingsProvider'
    );
  });

  it('throw for usePortionsPerDay', async () => {
    await expect(renderHook(() => usePortionsPerDay())).rejects.toThrow(
      'usePortionsPerDay must be used within SettingsProvider'
    );
  });
});
