import * as Crypto from 'expo-crypto';
import { create } from 'zustand';

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

export const useShoppingList = create<ShoppingListStore>((set) => ({
    items: [],
    toggleItem: (id) =>
        set((state) => ({
            items: state.items.map((i) =>
                i.id === id ? { ...i, checked: !i.checked } : i
            ),
        })),
    removeItem: (id) =>
        set((state) => ({
            items: state.items.filter((i) =>
                i.id !== id
            ),
        })),
    addItem: (newItem) => {
        set((state) => ({
            items: [...state.items, { id: Crypto.randomUUID(), ...newItem, checked: false }]
        }))
    }
}))
