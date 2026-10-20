import { fireEvent, render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';

import { ShoppingListItem } from './shopping-list-item';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

describe('ShoppingListItem', () => {
  it('renders the item label', async () => {
    await render(
      <ShoppingListItem id="1" label="Milk" checked={false} onToggle={jest.fn()} />,
      { wrapper }
    );

    expect(screen.getByText('Milk')).toBeTruthy();
  });

  it('calls onToggle when the row is pressed', async () => {
    const onToggle = jest.fn();
    await render(
      <ShoppingListItem id="1" label="Milk" checked={false} onToggle={onToggle} />,
      { wrapper }
    );

    await fireEvent.press(screen.getByText('Milk'));
    expect(onToggle).toHaveBeenCalledTimes(1);
  });

  it('strikes through checked items', async () => {
    await render(
      <ShoppingListItem id="1" label="Milk" checked onToggle={jest.fn()} />,
      { wrapper }
    );

    const flattened = StyleSheet.flatten(screen.getByText('Milk').props.style);
    expect(flattened.textDecorationLine).toBe('line-through');
  });

  it('does not strike through unchecked items', async () => {
    await render(
      <ShoppingListItem id="1" label="Milk" checked={false} onToggle={jest.fn()} />,
      { wrapper }
    );

    const flattened = StyleSheet.flatten(screen.getByText('Milk').props.style);
    expect(flattened.textDecorationLine).toBeUndefined();
  });
});
