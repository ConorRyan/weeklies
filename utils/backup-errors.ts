export type BackupFailureKind = 'export' | 'import';

function errorText(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '';
}

export function isUserCanceledBackup(error: unknown): boolean {
  const lower = errorText(error).toLowerCase();
  return (
    lower.includes('cancel') ||
    lower.includes('cancelled') ||
    lower.includes('canceled') ||
    lower.includes('user dismissed') ||
    lower.includes('dismissed')
  );
}

/** Maps thrown errors to short copy for toasts (no stack traces). */
export function formatBackupError(error: unknown, kind: BackupFailureKind): string {
  if (isUserCanceledBackup(error)) {
    return kind === 'export' ? 'Export was canceled.' : 'Import was canceled.';
  }

  const lower = errorText(error).toLowerCase();

  const domInterrupted =
    lower.includes('removechild') ||
    lower.includes('remove child') ||
    lower.includes('not a child') ||
    lower.includes('notfounderror') ||
    lower.includes('failed to execute');

  if (kind === 'export' && domInterrupted) {
    return 'The save was interrupted. Stay on Settings until the file finishes downloading.';
  }

  const permission =
    lower.includes('permission') ||
    lower.includes('eacces') ||
    lower.includes('access denied');

  if (permission) {
    return kind === 'export'
      ? 'Could not write there. Try another folder or check permissions.'
      : 'Could not read that file. Check permissions and try again.';
  }

  if (kind === 'import') {
    if (
      lower.includes('parse') ||
      lower.includes('invalid file') ||
      lower.includes('invalid backup') ||
      lower.includes('could not parse json')
    ) {
      return 'That file is not a valid Weeklies backup.';
    }
    if (lower.includes('unsupported backup')) {
      return 'That backup version is not supported.';
    }
    return 'Could not import. Pick a backup JSON file from this app.';
  }

  return 'Could not save your backup. Try again or choose another folder.';
}
