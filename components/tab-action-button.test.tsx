import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';

import { TabActionButton, TabActionRow } from './tab-action-button';

const mockPush = jest.fn();

jest.mock('expo-router', () => ({
  useRouter: () => ({ push: mockPush }),
}));

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

describe('TabActionButton', () => {
  beforeEach(() => {
    mockPush.mockClear();
  });

  it('renders its label', async () => {
    await render(<TabActionButton label="Import recipe" onPress={jest.fn()} />, { wrapper });

    expect(screen.getByText('Import recipe')).toBeTruthy();
  });

  it('calls onPress when tapped', async () => {
    const onPress = jest.fn();
    await render(<TabActionButton label="Import recipe" onPress={onPress} />, { wrapper });

    await fireEvent.press(screen.getByText('Import recipe'));

    expect(onPress).toHaveBeenCalledTimes(1);
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('navigates to href instead of calling onPress', async () => {
    const onPress = jest.fn();
    await render(<TabActionButton label="Add recipe" href="/recipe/add" onPress={onPress} />, {
      wrapper,
    });

    await fireEvent.press(screen.getByText('Add recipe'));

    expect(mockPush).toHaveBeenCalledTimes(1);
    expect(mockPush).toHaveBeenCalledWith('/recipe/add');
    expect(onPress).not.toHaveBeenCalled();
  });

  it('ignores taps when disabled', async () => {
    const onPress = jest.fn();
    await render(<TabActionButton label="Import recipe" onPress={onPress} disabled />, {
      wrapper,
    });

    await fireEvent.press(screen.getByText('Import recipe'));

    expect(onPress).not.toHaveBeenCalled();
    expect(mockPush).not.toHaveBeenCalled();
  });
});

describe('TabActionRow', () => {
  it('renders its children', async () => {
    await render(
      <TabActionRow>
        <TabActionButton label="Import recipe" onPress={jest.fn()} />
      </TabActionRow>,
      { wrapper }
    );

    expect(screen.getByText('Import recipe')).toBeTruthy();
  });
});
