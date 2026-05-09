import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Appearance } from 'react-native';

export const SETTINGS_STORAGE_KEY = 'weeklies.settings';

const CURRENT_SCHEMA_VERSION = 1;

const DEFAULT_PORTIONS_PER_DAY = 2;

export type AppColorScheme = 'light' | 'dark';

export type Settings = {
  colorScheme: AppColorScheme;
  portionsPerDay: number;
};

type PersistedSettings = Settings & {
  settingsSchemaVersion: number;
};

export function isValidPortionsPerDay(n: number): boolean {
  return Number.isFinite(n) && n > 0;
}

function initialFromOs(): AppColorScheme {
  return Appearance.getColorScheme() === 'light' ? 'light' : 'dark';
}

function defaultSettings(): Settings {
  return {
    colorScheme: initialFromOs(),
    portionsPerDay: DEFAULT_PORTIONS_PER_DAY,
  };
}

function extractPartialFromObject(o: Record<string, unknown>): Partial<Settings> {
  const out: Partial<Settings> = {};
  if (o.colorScheme === 'light' || o.colorScheme === 'dark') {
    out.colorScheme = o.colorScheme;
  }
  if (typeof o.portionsPerDay === 'number' && isValidPortionsPerDay(o.portionsPerDay)) {
    out.portionsPerDay = o.portionsPerDay;
  }
  return out;
}

/** Hook for future schema migrations; v1 is a straight extract. */
function migrateParsedRecord(schemaVersion: number, o: Record<string, unknown>): Partial<Settings> {
  void schemaVersion;
  return extractPartialFromObject(o);
}

export function parseStoredSettings(raw: string | null): Settings {
  const defaults = defaultSettings();
  if (raw == null || raw === '') {
    return defaults;
  }
  try {
    const data = JSON.parse(raw) as unknown;
    if (!data || typeof data !== 'object') {
      return defaults;
    }
    const o = data as Record<string, unknown>;
    const schemaVersion =
      typeof o.settingsSchemaVersion === 'number' && Number.isFinite(o.settingsSchemaVersion)
        ? Math.floor(o.settingsSchemaVersion)
        : 1;
    const partial = migrateParsedRecord(schemaVersion, o);
    return {
      colorScheme: partial.colorScheme ?? defaults.colorScheme,
      portionsPerDay: partial.portionsPerDay ?? defaults.portionsPerDay,
    };
  } catch {
    return defaults;
  }
}

async function readSettingsFromStorage(): Promise<Settings> {
  const raw = await AsyncStorage.getItem(SETTINGS_STORAGE_KEY);
  return parseStoredSettings(raw);
}

/** Serialized value stored under {@link SETTINGS_STORAGE_KEY}. */
export function serializeSettingsStorage(settings: Settings): string {
  const payload: PersistedSettings = {
    ...settings,
    settingsSchemaVersion: CURRENT_SCHEMA_VERSION,
  };
  return JSON.stringify(payload);
}

async function persistSettings(settings: Settings): Promise<void> {
  await AsyncStorage.setItem(SETTINGS_STORAGE_KEY, serializeSettingsStorage(settings));
}

type SettingsStoreContextValue = {
  settings: Settings;
  patchSettings: (partial: Partial<Settings>) => void;
  replaceSettings: (next: Settings) => void;
};

type ThemePreferenceContextValue = {
  colorScheme: AppColorScheme;
  setColorScheme: (scheme: AppColorScheme) => void;
};

type PortionsPreferenceContextValue = {
  portionsPerDay: number;
  setPortionsPerDay: (n: number) => void;
};

const SettingsStoreContext = createContext<SettingsStoreContextValue | null>(null);
const ThemePreferenceContext = createContext<ThemePreferenceContextValue | null>(null);
const PortionsPreferenceContext = createContext<PortionsPreferenceContextValue | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<Settings>(defaultSettings);
  const hasUserMutatedRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const loaded = await readSettingsFromStorage();
        if (cancelled) return;
        if (hasUserMutatedRef.current) {
          return;
        }
        setSettings(loaded);
        await persistSettings(loaded);
      } catch {
        /* keep initial defaults */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const patchSettings = useCallback((partial: Partial<Settings>) => {
    hasUserMutatedRef.current = true;
    setSettings((s) => {
      const next: Settings = { ...s };
      if (partial.colorScheme !== undefined) {
        if (partial.colorScheme === 'light' || partial.colorScheme === 'dark') {
          next.colorScheme = partial.colorScheme;
        }
      }
      if (partial.portionsPerDay !== undefined) {
        if (isValidPortionsPerDay(partial.portionsPerDay)) {
          next.portionsPerDay = partial.portionsPerDay;
        }
      }
      if (next.colorScheme === s.colorScheme && next.portionsPerDay === s.portionsPerDay) {
        return s;
      }
      void persistSettings(next);
      return next;
    });
  }, []);

  const replaceSettings = useCallback((next: Settings) => {
    hasUserMutatedRef.current = true;
    const validated: Settings = {
      colorScheme: next.colorScheme === 'light' || next.colorScheme === 'dark' ? next.colorScheme : defaultSettings().colorScheme,
      portionsPerDay: isValidPortionsPerDay(next.portionsPerDay) ? next.portionsPerDay : defaultSettings().portionsPerDay,
    };
    setSettings(validated);
    void persistSettings(validated);
  }, []);

  const setColorScheme = useCallback(
    (scheme: AppColorScheme) => {
      patchSettings({ colorScheme: scheme });
    },
    [patchSettings]
  );

  const setPortionsPerDay = useCallback(
    (n: number) => {
      if (!isValidPortionsPerDay(n)) {
        return;
      }
      patchSettings({ portionsPerDay: n });
    },
    [patchSettings]
  );

  const storeValue = useMemo(
    () => ({ settings, patchSettings, replaceSettings }),
    [settings, patchSettings, replaceSettings]
  );

  const themeValue = useMemo(
    () => ({ colorScheme: settings.colorScheme, setColorScheme }),
    [settings.colorScheme, setColorScheme]
  );

  const portionsValue = useMemo(
    () => ({ portionsPerDay: settings.portionsPerDay, setPortionsPerDay }),
    [settings.portionsPerDay, setPortionsPerDay]
  );

  return (
    <SettingsStoreContext.Provider value={storeValue}>
      <ThemePreferenceContext.Provider value={themeValue}>
        <PortionsPreferenceContext.Provider value={portionsValue}>
          {children}
        </PortionsPreferenceContext.Provider>
      </ThemePreferenceContext.Provider>
    </SettingsStoreContext.Provider>
  );
}

export function useSettings(): SettingsStoreContextValue {
  const ctx = useContext(SettingsStoreContext);
  if (!ctx) {
    throw new Error('useSettings must be used within SettingsProvider');
  }
  return ctx;
}

export function useThemePreference(): ThemePreferenceContextValue {
  const ctx = useContext(ThemePreferenceContext);
  if (!ctx) {
    throw new Error('useThemePreference must be used within SettingsProvider');
  }
  return ctx;
}

export function usePortionsPerDay(): PortionsPreferenceContextValue {
  const ctx = useContext(PortionsPreferenceContext);
  if (!ctx) {
    throw new Error('usePortionsPerDay must be used within SettingsProvider');
  }
  return ctx;
}
