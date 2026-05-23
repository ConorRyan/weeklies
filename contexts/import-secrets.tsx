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

export const IMPORT_SECRETS_STORAGE_KEY = 'weeklies.secrets:v1';

export type ImportSecrets = {
  mistralApiKey: string;
};

type PersistedImportSecrets = ImportSecrets & {
  secretsSchemaVersion: number;
};

const CURRENT_SCHEMA_VERSION = 1;

const EMPTY_SECRETS: ImportSecrets = {
  mistralApiKey: '',
};

function extractPartialFromObject(o: Record<string, unknown>): Partial<ImportSecrets> {
  const out: Partial<ImportSecrets> = {};
  if (typeof o.mistralApiKey === 'string') {
    out.mistralApiKey = o.mistralApiKey;
  }
  return out;
}

function migrateParsedRecord(schemaVersion: number, o: Record<string, unknown>): Partial<ImportSecrets> {
  void schemaVersion;
  return extractPartialFromObject(o);
}

export function parseStoredImportSecrets(raw: string | null): ImportSecrets {
  if (raw == null || raw === '') {
    return { ...EMPTY_SECRETS };
  }
  try {
    const data = JSON.parse(raw) as unknown;
    if (!data || typeof data !== 'object') {
      return { ...EMPTY_SECRETS };
    }
    const o = data as Record<string, unknown>;
    const schemaVersion =
      typeof o.secretsSchemaVersion === 'number' && Number.isFinite(o.secretsSchemaVersion)
        ? Math.floor(o.secretsSchemaVersion)
        : 1;
    const partial = migrateParsedRecord(schemaVersion, o);
    return {
      mistralApiKey: partial.mistralApiKey ?? '',
    };
  } catch {
    return { ...EMPTY_SECRETS };
  }
}

async function readImportSecretsFromStorage(): Promise<ImportSecrets> {
  const raw = await AsyncStorage.getItem(IMPORT_SECRETS_STORAGE_KEY);
  return parseStoredImportSecrets(raw);
}

export function serializeImportSecretsStorage(secrets: ImportSecrets): string {
  const payload: PersistedImportSecrets = {
    mistralApiKey: secrets.mistralApiKey,
    secretsSchemaVersion: CURRENT_SCHEMA_VERSION,
  };
  return JSON.stringify(payload);
}

async function persistImportSecrets(secrets: ImportSecrets): Promise<void> {
  await AsyncStorage.setItem(IMPORT_SECRETS_STORAGE_KEY, serializeImportSecretsStorage(secrets));
}

export function hasMistralApiKey(secrets: ImportSecrets): boolean {
  return secrets.mistralApiKey.trim().length > 0;
}

type ImportSecretsContextValue = {
  secrets: ImportSecrets;
  setMistralApiKey: (key: string) => void;
  clearMistralApiKey: () => void;
};

const ImportSecretsContext = createContext<ImportSecretsContextValue | null>(null);

export function ImportSecretsProvider({ children }: { children: ReactNode }) {
  const [secrets, setSecrets] = useState<ImportSecrets>(EMPTY_SECRETS);
  const hasUserMutatedRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const loaded = await readImportSecretsFromStorage();
        if (cancelled || hasUserMutatedRef.current) {
          return;
        }
        setSecrets(loaded);
      } catch {
        /* keep empty */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const setMistralApiKey = useCallback((key: string) => {
    hasUserMutatedRef.current = true;
    setSecrets((prev) => {
      const next = { mistralApiKey: key };
      if (prev.mistralApiKey === next.mistralApiKey) {
        return prev;
      }
      void persistImportSecrets(next);
      return next;
    });
  }, []);

  const clearMistralApiKey = useCallback(() => {
    hasUserMutatedRef.current = true;
    setSecrets((prev) => {
      if (prev.mistralApiKey === '') {
        return prev;
      }
      const next = { mistralApiKey: '' };
      void persistImportSecrets(next);
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ secrets, setMistralApiKey, clearMistralApiKey }),
    [secrets, setMistralApiKey, clearMistralApiKey]
  );

  return <ImportSecretsContext.Provider value={value}>{children}</ImportSecretsContext.Provider>;
}

export function useImportSecrets(): ImportSecretsContextValue {
  const ctx = useContext(ImportSecretsContext);
  if (!ctx) {
    throw new Error('useImportSecrets must be used within ImportSecretsProvider');
  }
  return ctx;
}
