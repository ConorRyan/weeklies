import { formatBackupError, isUserCanceledBackup } from '@/utils/backup-errors';

describe('isUserCanceledBackup', () => {
  it('detects cancel-related messages', () => {
    expect(isUserCanceledBackup(new Error('User canceled'))).toBe(true);
    expect(isUserCanceledBackup(new Error('Operation cancelled'))).toBe(true);
    expect(isUserCanceledBackup(new Error('User dismissed picker'))).toBe(true);
  });

  it('returns false for unrelated errors', () => {
    expect(isUserCanceledBackup(new Error('Network failed'))).toBe(false);
  });
});

describe('formatBackupError', () => {
  it('formats canceled export/import', () => {
    expect(formatBackupError(new Error('canceled'), 'export')).toBe('Export was canceled.');
    expect(formatBackupError(new Error('cancelled'), 'import')).toBe('Import was canceled.');
  });

  it('formats invalid backup on import', () => {
    expect(formatBackupError(new Error('Could not parse JSON'), 'import')).toBe(
      'That file is not a valid Weeklies backup.'
    );
    expect(formatBackupError(new Error('Unsupported backup version'), 'import')).toBe(
      'That backup version is not supported.'
    );
  });

  it('formats permission errors', () => {
    expect(formatBackupError(new Error('EACCES permission denied'), 'export')).toBe(
      'Could not write there. Try another folder or check permissions.'
    );
  });

  it('formats DOM-interrupted export', () => {
    expect(formatBackupError(new Error('Failed to execute removeChild'), 'export')).toBe(
      'The save was interrupted. Stay on Settings until the file finishes downloading.'
    );
  });

  it('falls back to generic export message', () => {
    expect(formatBackupError(new Error('unknown'), 'export')).toBe(
      'Could not save your backup. Try again or choose another folder.'
    );
  });

  it('falls back to generic import message', () => {
    expect(formatBackupError(new Error('unknown'), 'import')).toBe(
      'Could not import. Pick a backup JSON file from this app.'
    );
  });
});
