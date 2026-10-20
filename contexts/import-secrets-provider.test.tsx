import { act, renderHook, waitFor } from '@testing-library/react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import type { ReactNode } from 'react';

import {
  IMPORT_SECRETS_STORAGE_KEY,
  ImportSecretsProvider,
  serializeImportSecretsStorage,
  useImportSecrets,
} from '@/contexts/import-secrets';

const wrapper = ({ children }: { children: ReactNode }) => (
  <ImportSecretsProvider>{children}</ImportSecretsProvider>
);

describe('ImportSecretsProvider', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  it('starts with an empty API key', async () => {
    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    expect(result.current.secrets.mistralApiKey).toBe('');
  });

  it('hydrates a persisted API key', async () => {
    await AsyncStorage.setItem(
      IMPORT_SECRETS_STORAGE_KEY,
      serializeImportSecretsStorage({ mistralApiKey: 'stored-key' })
    );

    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    await waitFor(() => expect(result.current.secrets.mistralApiKey).toBe('stored-key'));
  });

  it('persists a newly set API key', async () => {
    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    await act(async () => {
      result.current.setMistralApiKey('my-key');
    });

    expect(JSON.parse(String(await AsyncStorage.getItem(IMPORT_SECRETS_STORAGE_KEY)))).toEqual({
      mistralApiKey: 'my-key',
      secretsSchemaVersion: 1,
    });
  });

  it('does not rewrite storage when the key is unchanged', async () => {
    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    await act(async () => {
      result.current.setMistralApiKey('my-key');
    });

    const setItemSpy = jest.spyOn(AsyncStorage, 'setItem');
    setItemSpy.mockClear();

    await act(async () => {
      result.current.setMistralApiKey('my-key');
    });

    expect(setItemSpy).not.toHaveBeenCalled();
    jest.restoreAllMocks();
  });

  it('clears the API key and persists the empty value', async () => {
    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    await act(async () => {
      result.current.setMistralApiKey('my-key');
    });
    await act(async () => {
      result.current.clearMistralApiKey();
    });

    expect(result.current.secrets.mistralApiKey).toBe('');
    expect(JSON.parse(String(await AsyncStorage.getItem(IMPORT_SECRETS_STORAGE_KEY)))).toEqual({
      mistralApiKey: '',
      secretsSchemaVersion: 1,
    });
  });

  it('keeps the user mutation when hydration lands afterwards', async () => {
    await AsyncStorage.setItem(
      IMPORT_SECRETS_STORAGE_KEY,
      serializeImportSecretsStorage({ mistralApiKey: 'stored-key' })
    );

    const { result } = await renderHook(() => useImportSecrets(), { wrapper });

    await act(async () => {
      result.current.setMistralApiKey('typed-key');
    });
    await act(async () => {});

    expect(result.current.secrets.mistralApiKey).toBe('typed-key');
  });
});

describe('useImportSecrets without a provider', () => {
  it('throws', async () => {
    await expect(renderHook(() => useImportSecrets())).rejects.toThrow(
      'useImportSecrets must be used within ImportSecretsProvider'
    );
  });
});
