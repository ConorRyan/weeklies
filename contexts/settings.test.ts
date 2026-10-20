import {
  parseStoredSettings,
  serializeSettingsStorage,
  type Settings,
} from '@/contexts/settings';

jest.mock('react-native', () => ({
  Appearance: { getColorScheme: () => 'dark' },
  Platform: { OS: 'ios', select: ({ ios }: { ios: unknown }) => ios },
}));

describe('parseStoredSettings', () => {
  it('returns defaults for null or empty input', () => {
    const defaults = parseStoredSettings(null);
    expect(defaults.portionsPerDay).toBe(2);
    expect(['light', 'dark']).toContain(defaults.colorScheme);
  });

  it('parses valid stored settings', () => {
    const raw = serializeSettingsStorage({ colorScheme: 'light', portionsPerDay: 3 });
    expect(parseStoredSettings(raw)).toEqual({ colorScheme: 'light', portionsPerDay: 3 });
  });

  it('ignores invalid portions', () => {
    const raw = JSON.stringify({
      colorScheme: 'dark',
      portionsPerDay: -1,
      settingsSchemaVersion: 1,
    });
    const result = parseStoredSettings(raw);
    expect(result.portionsPerDay).toBe(2);
  });

  it('ignores invalid color scheme', () => {
    const raw = JSON.stringify({
      colorScheme: 'purple',
      portionsPerDay: 3,
      settingsSchemaVersion: 1,
    });
    const result = parseStoredSettings(raw);
    expect(['light', 'dark']).toContain(result.colorScheme);
    expect(result.portionsPerDay).toBe(3);
  });

  it('returns defaults for corrupt JSON', () => {
    const defaults = parseStoredSettings('{bad json');
    expect(defaults.portionsPerDay).toBe(2);
  });
});

describe('serializeSettingsStorage', () => {
  it('round-trips through parseStoredSettings', () => {
    const settings: Settings = { colorScheme: 'dark', portionsPerDay: 4 };
    const parsed = parseStoredSettings(serializeSettingsStorage(settings));
    expect(parsed).toEqual(settings);
  });

  it('includes schema version', () => {
    const raw = serializeSettingsStorage({ colorScheme: 'light', portionsPerDay: 2 });
    const data = JSON.parse(raw) as { settingsSchemaVersion: number };
    expect(data.settingsSchemaVersion).toBe(1);
  });
});
