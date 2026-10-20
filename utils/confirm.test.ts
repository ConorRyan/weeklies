import { Alert, Platform } from 'react-native';

import { confirmAction } from '@/utils/confirm';

jest.mock('react-native', () => ({
  Alert: { alert: jest.fn() },
  Platform: { OS: 'ios', select: ({ ios }: { ios: unknown }) => ios },
}));

const alertMock = Alert.alert as unknown as jest.Mock;

type AlertButton = { text: string; style?: string; onPress?: () => void };

const setOS = (os: string) => {
  Object.defineProperty(Platform, 'OS', { value: os, configurable: true, writable: true });
};

describe('confirmAction (native)', () => {
  beforeEach(() => {
    alertMock.mockClear();
    setOS('ios');
  });

  it('alerts with the given title and message', () => {
    confirmAction({ title: 'Delete recipe?', message: 'This cannot be undone.', onConfirm: jest.fn() });

    expect(alertMock).toHaveBeenCalledWith(
      'Delete recipe?',
      'This cannot be undone.',
      expect.any(Array)
    );
  });

  it('runs onConfirm only when the confirm button is pressed', () => {
    const onConfirm = jest.fn();
    confirmAction({ title: 'T', message: 'M', onConfirm });

    const buttons = alertMock.mock.calls[0][2] as AlertButton[];
    buttons[0].onPress?.();
    expect(onConfirm).not.toHaveBeenCalled();

    buttons[1].onPress?.();
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('uses custom labels and a destructive style when requested', () => {
    confirmAction({
      title: 'T',
      message: 'M',
      confirmLabel: 'Delete',
      cancelLabel: 'Keep it',
      destructive: true,
      onConfirm: jest.fn(),
    });

    const buttons = alertMock.mock.calls[0][2] as AlertButton[];
    expect(buttons[0]).toMatchObject({ text: 'Keep it', style: 'cancel' });
    expect(buttons[1]).toMatchObject({ text: 'Delete', style: 'destructive' });
  });

  it('defaults to OK and Cancel', () => {
    confirmAction({ title: 'T', message: 'M', onConfirm: jest.fn() });

    const buttons = alertMock.mock.calls[0][2] as AlertButton[];
    expect(buttons[0]).toMatchObject({ text: 'Cancel' });
    expect(buttons[1]).toMatchObject({ text: 'OK', style: 'default' });
  });
});

describe('confirmAction (web)', () => {
  const originalWindow = (globalThis as { window?: unknown }).window;

  afterEach(() => {
    (globalThis as { window?: unknown }).window = originalWindow;
    setOS('ios');
  });

  it('confirms via window.confirm and runs onConfirm', () => {
    setOS('web');
    const confirmSpy = jest.fn(() => true);
    (globalThis as { window: unknown }).window = { confirm: confirmSpy };
    const onConfirm = jest.fn();

    confirmAction({ title: 'Delete recipe?', message: 'Are you sure?', onConfirm });

    expect(confirmSpy).toHaveBeenCalledWith('Delete recipe?\n\nAre you sure?');
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('does nothing when the user cancels', () => {
    setOS('web');
    (globalThis as { window: unknown }).window = { confirm: jest.fn(() => false) };
    const onConfirm = jest.fn();

    confirmAction({ title: 'T', message: 'M', onConfirm });

    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('is a no-op when window is unavailable', () => {
    setOS('web');
    (globalThis as { window?: unknown }).window = undefined;
    const onConfirm = jest.fn();

    expect(() => confirmAction({ title: 'T', message: 'M', onConfirm })).not.toThrow();
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
