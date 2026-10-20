import { act, renderHook, waitFor } from '@testing-library/react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { ReactNode } from 'react';

import { Colors } from '@/constants/theme';
import { SettingsProvider, useThemePreference } from '@/contexts/settings';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { useThemeColor } from '@/hooks/use-theme-color';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

function useProbe() {
  return {
    scheme: useAppColorScheme(),
    tint: useThemeColor({}, 'tint'),
    custom: useThemeColor({ light: '#111111', dark: '#222222' }, 'tint'),
    themePreference: useThemePreference(),
  };
}

describe('useThemeColor / useAppColorScheme', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  it('starts on the hydration-safe dark scheme', async () => {
    const { result } = await renderHook(() => useProbe(), { wrapper });

    expect(result.current.scheme).toBe('dark');
    expect(result.current.tint).toBe(Colors.dark.tint);
  });

  it('prefers explicit light/dark colors over the palette', async () => {
    const { result } = await renderHook(() => useProbe(), { wrapper });

    expect(result.current.custom).toBe('#222222');
  });

  it('re-resolves colors when the scheme changes', async () => {
    const { result } = await renderHook(() => useProbe(), { wrapper });

    await act(async () => {
      result.current.themePreference.setColorScheme('light');
    });

    await waitFor(() => expect(result.current.scheme).toBe('light'));
    expect(result.current.tint).toBe(Colors.light.tint);
    expect(result.current.custom).toBe('#111111');
  });

  it('keeps the user choice when stored settings hydrate late', async () => {
    const { result } = await renderHook(() => useProbe(), { wrapper });

    await act(async () => {
      result.current.themePreference.setColorScheme('light');
    });
    // Flush the provider's async hydration effect.
    await act(async () => {});

    expect(result.current.scheme).toBe('light');
  });
});
