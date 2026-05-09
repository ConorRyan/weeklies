import * as Crypto from 'expo-crypto';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { SHOPPING_LIST_STORAGE_KEY, shoppingListStorage } from '@/store/persistence';

export type NewItem = {
  label: string;
};

export type Item = {
  id: string;
  label: string;
  checked: boolean;
};

type ShoppingListStore = {
  items: Item[];
  toggleItem: (id: string) => void;
  removeItem: (id: string) => void;
  removeCheckedItems: () => void;
  addItem: (item: NewItem) => void;
};

export const normalizeItem = (value: unknown): Item | null => {
  if (!value || typeof value !== 'object') {
    return null;
  }
  const i = value as Record<string, unknown>;
  if (typeof i.id !== 'string' || typeof i.checked !== 'boolean') {
    return null;
  }
  if (typeof i.label === 'string') {
    return { id: i.id, label: i.label, checked: i.checked };
  }
  if (typeof i.name === 'string' && typeof i.quantity === 'string') {
    const name = i.name.trim();
    const quantity = i.quantity.trim();
    const label = [quantity, name].filter((s) => s.length > 0).join(' ');
    const resolved = label.length > 0 ? label : name || quantity;
    if (resolved.length === 0) {
      return null;
    }
    return { id: i.id, label: resolved, checked: i.checked };
  }
  return null;
};

const isItem = (value: unknown): value is Item =>
  !!value &&
  typeof value === 'object' &&
  typeof (value as Item).id === 'string' &&
  typeof (value as Item).label === 'string' &&
  typeof (value as Item).checked === 'boolean';

export const useShoppingList = create<ShoppingListStore>()(
  persist(
    (set) => ({
      items: [],
      toggleItem: (id) =>
        set((state) => ({
          items: state.items.map((item) =>
            item.id === id ? { ...item, checked: !item.checked } : item
          ),
        })),
      removeItem: (id) =>
        set((state) => ({
          items: state.items.filter((item) => item.id !== id),
        })),
      removeCheckedItems: () =>
        set((state) => ({
          items: state.items.filter((item) => !item.checked),
        })),
      addItem: (newItem) => {
        set((state) => ({
          items: [
            ...state.items,
            { id: Crypto.randomUUID(), ...newItem, checked: false },
          ],
        }));
      },
    }),
    {
      name: SHOPPING_LIST_STORAGE_KEY,
      storage: shoppingListStorage,
      version: 2,
      partialize: (state) => ({
        items: state.items,
      }),
      migrate: (persistedState) => {
        const items = (persistedState as { items?: unknown } | undefined)?.items;
        if (!Array.isArray(items)) {
          return { items: [] };
        }

        return {
          items: items
            .map(normalizeItem)
            .filter((item): item is Item => item !== null)
            .filter(isItem),
        };
      },
    }
  )
);
