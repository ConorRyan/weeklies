import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';
import { useShoppingList } from '@/store/shopping-list';

import { ShoppingListAddLine } from './shopping-list-add-line';

jest.mock('expo-crypto', () => ({
  randomUUID: jest.fn(() => 'item-uuid-1'),
}));

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

describe('ShoppingListAddLine', () => {
  beforeEach(() => {
    useShoppingList.setState({ items: [] });
  });

  it('adds a trimmed item to the shopping list on submit', async () => {
    const { getByRole } = await render(<ShoppingListAddLine />, { wrapper });

    await fireEvent.changeText(screen.getByPlaceholderText('Item'), '  Bread ');
    await fireEvent.press(getByRole('button'));

    expect(useShoppingList.getState().items).toEqual([
      { id: 'item-uuid-1', label: 'Bread', checked: false },
    ]);
  });

  it('ignores empty and whitespace-only submissions', async () => {
    const { getByRole } = await render(<ShoppingListAddLine />, { wrapper });

    await fireEvent.changeText(screen.getByPlaceholderText('Item'), '   ');
    await fireEvent.press(getByRole('button'));

    expect(useShoppingList.getState().items).toEqual([]);
  });

  it('clears the input after adding', async () => {
    const { getByRole } = await render(<ShoppingListAddLine />, { wrapper });

    await fireEvent.changeText(screen.getByPlaceholderText('Item'), 'Bread');
    await fireEvent.press(getByRole('button'));

    expect(screen.getByPlaceholderText('Item').props.value).toBe('');
  });
});
