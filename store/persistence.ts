import AsyncStorage from '@react-native-async-storage/async-storage';
import { createJSONStorage, type StateStorage } from 'zustand/middleware';

export const RECIPES_STORAGE_KEY = 'weeklies:recipes:v1';
export const SHOPPING_LIST_STORAGE_KEY = 'weeklies:shopping-list:v1';
export const WEEKLY_PLAN_STORAGE_KEY = 'weeklies:weekly-plan:v1';

const SHOPPING_LIST_DEBOUNCE_MS = 400;

type PendingWrite = {
  value: string;
  timer: ReturnType<typeof setTimeout>;
  waiting: Array<{
    resolve: () => void;
    reject: (error: unknown) => void;
  }>;
};

const pendingWrites = new Map<string, PendingWrite>();

const isShoppingListKey = (key: string) => key === SHOPPING_LIST_STORAGE_KEY;

const flushKey = async (key: string) => {
  const pending = pendingWrites.get(key);
  if (!pending) {
    return;
  }

  clearTimeout(pending.timer);
  pendingWrites.delete(key);

  try {
    await AsyncStorage.setItem(key, pending.value);
    pending.waiting.forEach(({ resolve }) => resolve());
  } catch (error) {
    pending.waiting.forEach(({ reject }) => reject(error));
  }
};

const scheduleDebouncedWrite = (key: string, value: string) =>
  new Promise<void>((resolve, reject) => {
    const existing = pendingWrites.get(key);

    if (existing) {
      clearTimeout(existing.timer);
      existing.value = value;
      existing.waiting.push({ resolve, reject });
      existing.timer = setTimeout(() => {
        void flushKey(key);
      }, SHOPPING_LIST_DEBOUNCE_MS);
      return;
    }

    pendingWrites.set(key, {
      value,
      waiting: [{ resolve, reject }],
      timer: setTimeout(() => {
        void flushKey(key);
      }, SHOPPING_LIST_DEBOUNCE_MS),
    });
  });

const debouncedStateStorage: StateStorage = {
  getItem: async (key) => {
    if (isShoppingListKey(key)) {
      await flushKey(key);
    }
    return AsyncStorage.getItem(key);
  },
  setItem: (key, value) => {
    if (!isShoppingListKey(key)) {
      return AsyncStorage.setItem(key, value);
    }
    return scheduleDebouncedWrite(key, value);
  },
  removeItem: async (key) => {
    if (isShoppingListKey(key)) {
      await flushKey(key);
    }
    return AsyncStorage.removeItem(key);
  },
};

export const recipesStorage = createJSONStorage(() => AsyncStorage);
export const weeklyPlanStorage = createJSONStorage(() => AsyncStorage);
export const shoppingListStorage = createJSONStorage(() => debouncedStateStorage);

export const flushPendingShoppingListWrites = async () => {
  await flushKey(SHOPPING_LIST_STORAGE_KEY);
};
