import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { Appearance } from 'react-native';

const STORAGE_KEY = 'weeklies.theme';

export type AppColorScheme = 'light' | 'dark';

type ThemePreferenceContextValue = {
  colorScheme: AppColorScheme;
  setColorScheme: (scheme: AppColorScheme) => void;
};

const ThemePreferenceContext = createContext<ThemePreferenceContextValue | null>(null);

function initialFromOs(): AppColorScheme {
  return Appearance.getColorScheme() === 'light' ? 'light' : 'dark';
}

export function ThemePreferenceProvider({ children }: { children: ReactNode }) {
  const [colorScheme, setColorSchemeState] = useState<AppColorScheme>(initialFromOs);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const stored = await AsyncStorage.getItem(STORAGE_KEY);
        if (cancelled) return;
        if (stored === 'light' || stored === 'dark') {
          setColorSchemeState(stored);
        }
      } catch {
        /* keep current scheme */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const setColorScheme = useCallback((scheme: AppColorScheme) => {
    setColorSchemeState(scheme);
    void AsyncStorage.setItem(STORAGE_KEY, scheme);
  }, []);

  const value = useMemo(
    () => ({ colorScheme, setColorScheme }),
    [colorScheme, setColorScheme]
  );

  return (
    <ThemePreferenceContext.Provider value={value}>{children}</ThemePreferenceContext.Provider>
  );
}

export function useThemePreference(): ThemePreferenceContextValue {
  const ctx = useContext(ThemePreferenceContext);
  if (!ctx) {
    throw new Error('useThemePreference must be used within ThemePreferenceProvider');
  }
  return ctx;
}
