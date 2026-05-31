jest.mock('expo-crypto', () => ({
  randomUUID: jest.fn(() => 'item-uuid-1'),
}));

import { useShoppingList } from '@/store/shopping-list';

describe('useShoppingList actions', () => {
  beforeEach(() => {
    useShoppingList.setState({ items: [] });
  });

  it('adds an item with generated id', () => {
    useShoppingList.getState().addItem({ label: 'Milk' });
    expect(useShoppingList.getState().items).toEqual([
      { id: 'item-uuid-1', label: 'Milk', checked: false },
    ]);
  });

  it('toggles item checked state', () => {
    useShoppingList.setState({
      items: [{ id: '1', label: 'Milk', checked: false }],
    });
    useShoppingList.getState().toggleItem('1');
    expect(useShoppingList.getState().items[0].checked).toBe(true);
  });

  it('removes checked items', () => {
    useShoppingList.setState({
      items: [
        { id: '1', label: 'Milk', checked: true },
        { id: '2', label: 'Eggs', checked: false },
      ],
    });
    useShoppingList.getState().removeCheckedItems();
    expect(useShoppingList.getState().items).toEqual([
      { id: '2', label: 'Eggs', checked: false },
    ]);
  });

  it('removes item by id', () => {
    useShoppingList.setState({
      items: [
        { id: '1', label: 'Milk', checked: false },
        { id: '2', label: 'Eggs', checked: false },
      ],
    });
    useShoppingList.getState().removeItem('1');
    expect(useShoppingList.getState().items).toHaveLength(1);
  });
});
