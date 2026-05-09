import * as Crypto from 'expo-crypto';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { SHOPPING_LIST_STORAGE_KEY, shoppingListStorage } from '@/store/persistence';

export type NewItem = {
    name: string;
    quantity: string;
};

type Item = {
    id: string;
    name: string;
    quantity: string;
    checked: boolean;
};

type ShoppingListStore = {
    items: Item[];
    toggleItem: (id: string) => void;
    removeItem: (id: string) => void;
    addItem: (item: NewItem) => void;
};

const isItem = (value: unknown): value is Item =>
  !!value &&
  typeof value === 'object' &&
  typeof (value as Item).id === 'string' &&
  typeof (value as Item).name === 'string' &&
  typeof (value as Item).quantity === 'string' &&
  typeof (value as Item).checked === 'boolean';

export const useShoppingList = create<ShoppingListStore>()(
  persist(
    (set) => ({
      items: [],
      toggleItem: (id) =>
        set((state) => ({
          items: state.items.map((i) =>
            i.id === id ? { ...i, checked: !i.checked } : i
          ),
        })),
      removeItem: (id) =>
        set((state) => ({
          items: state.items.filter((i) => i.id !== id),
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
      version: 1,
      partialize: (state) => ({
        items: state.items,
      }),
      migrate: (persistedState) => {
        const items = (persistedState as { items?: unknown } | undefined)?.items;
        if (!Array.isArray(items)) {
          return { items: [] };
        }

        return {
          items: items.filter(isItem),
        };
      },
    }
  )
);
