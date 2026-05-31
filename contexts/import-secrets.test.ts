import {
  hasMistralApiKey,
  parseStoredImportSecrets,
  serializeImportSecretsStorage,
} from '@/contexts/import-secrets';

describe('parseStoredImportSecrets', () => {
  it('returns empty secrets for null or empty input', () => {
    expect(parseStoredImportSecrets(null)).toEqual({ mistralApiKey: '' });
    expect(parseStoredImportSecrets('')).toEqual({ mistralApiKey: '' });
  });

  it('parses valid stored secrets', () => {
    const raw = serializeImportSecretsStorage({ mistralApiKey: 'sk-test' });
    expect(parseStoredImportSecrets(raw)).toEqual({ mistralApiKey: 'sk-test' });
  });

  it('returns empty secrets for corrupt JSON', () => {
    expect(parseStoredImportSecrets('{bad')).toEqual({ mistralApiKey: '' });
  });

  it('returns empty secrets for non-object JSON', () => {
    expect(parseStoredImportSecrets('"string"')).toEqual({ mistralApiKey: '' });
  });
});

describe('serializeImportSecretsStorage', () => {
  it('round-trips through parseStoredImportSecrets', () => {
    const secrets = { mistralApiKey: 'sk-abc' };
    expect(parseStoredImportSecrets(serializeImportSecretsStorage(secrets))).toEqual(secrets);
  });
});

describe('hasMistralApiKey', () => {
  it('returns true when key has non-whitespace content', () => {
    expect(hasMistralApiKey({ mistralApiKey: 'sk-test' })).toBe(true);
  });

  it('returns false for empty or whitespace-only key', () => {
    expect(hasMistralApiKey({ mistralApiKey: '' })).toBe(false);
    expect(hasMistralApiKey({ mistralApiKey: '   ' })).toBe(false);
  });
});
