import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';

import {
  LIST_SQUARE_ACTION_COMPACT_SIZE,
  ListSquareActionButton,
} from './list-square-action-button';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

const accent = { light: '#0a7ea4', dark: '#0a7ea4' };

describe('ListSquareActionButton', () => {
  it('renders the label and calls onPress', async () => {
    const onPress = jest.fn();
    const { getByRole } = await render(
      <ListSquareActionButton label="+" accent={accent} onPress={onPress} />,
      { wrapper }
    );

    expect(screen.getByText('+')).toBeTruthy();

    await fireEvent.press(getByRole('button'));
    expect(onPress).toHaveBeenCalledTimes(1);
  });

  it('does not call onPress when disabled', async () => {
    const onPress = jest.fn();
    const { getByRole } = await render(
      <ListSquareActionButton label="-" accent={accent} onPress={onPress} disabled />,
      { wrapper }
    );

    await fireEvent.press(getByRole('button'));
    expect(onPress).not.toHaveBeenCalled();
  });

  it('adds touch padding around compact sizes', async () => {
    const { getByRole } = await render(
      <ListSquareActionButton
        label="+"
        accent={accent}
        onPress={jest.fn()}
        size={LIST_SQUARE_ACTION_COMPACT_SIZE}
      />,
      { wrapper }
    );

    expect(getByRole('button').props.hitSlop).toEqual({
      top: 8,
      bottom: 8,
      left: 8,
      right: 8,
    });
  });

  it('has no touch padding at the default size', async () => {
    const { getByRole } = await render(
      <ListSquareActionButton label="+" accent={accent} onPress={jest.fn()} />,
      { wrapper }
    );

    expect(getByRole('button').props.hitSlop).toBeUndefined();
  });
});
