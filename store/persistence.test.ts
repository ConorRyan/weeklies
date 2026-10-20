import AsyncStorage from '@react-native-async-storage/async-storage';

import {
  RECIPES_STORAGE_KEY,
  SHOPPING_LIST_STORAGE_KEY,
  flushPendingShoppingListWrites,
  recipesStorage,
  shoppingListStorage,
} from '@/store/persistence';

// The zustand storages type their values as StorageValue objects and are
// possibly-undefined; tests exercise the raw JSON string API.
type JsonStorageAPI = {
  getItem: (key: string) => Promise<string | null>;
  setItem: (key: string, value: string) => Promise<void>;
  removeItem: (key: string) => Promise<void>;
};

const shoppingList = shoppingListStorage as unknown as JsonStorageAPI;
const recipes = recipesStorage as unknown as JsonStorageAPI;

describe('persistence storages', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(async () => {
    await flushPendingShoppingListWrites();
    jest.useRealTimers();
    await AsyncStorage.clear();
    jest.restoreAllMocks();
  });

  describe('shopping list (debounced)', () => {
    it('does not write before the debounce window elapses', async () => {
      const pending = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'a');

      await jest.advanceTimersByTimeAsync(200);
      expect(await AsyncStorage.getItem(SHOPPING_LIST_STORAGE_KEY)).toBeNull();

      await jest.advanceTimersByTimeAsync(300);
      await pending;
      expect(await AsyncStorage.getItem(SHOPPING_LIST_STORAGE_KEY)).toBe('"a"');
    });

    it('coalesces rapid writes into a single write with the latest value', async () => {
      const setItemSpy = jest.spyOn(AsyncStorage, 'setItem');
      // The AsyncStorage mock's setItem is itself a jest.fn, so spying shares
      // its call history with earlier tests in this file.
      setItemSpy.mockClear();

      const first = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'a');
      const second = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'b');

      await jest.advanceTimersByTimeAsync(100);
      expect(setItemSpy).not.toHaveBeenCalled();

      await jest.advanceTimersByTimeAsync(400);
      await Promise.all([first, second]);

      expect(setItemSpy).toHaveBeenCalledTimes(1);
      expect(setItemSpy).toHaveBeenCalledWith(SHOPPING_LIST_STORAGE_KEY, '"b"');
    });

    it('flushes the pending write before getItem reads', async () => {
      const pending = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'latest');

      const value = await shoppingList.getItem(SHOPPING_LIST_STORAGE_KEY);
      await pending;

      expect(value).toBe('latest');
      expect(await AsyncStorage.getItem(SHOPPING_LIST_STORAGE_KEY)).toBe('"latest"');
    });

    it('flushes the pending write before removeItem removes', async () => {
      const pending = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'latest');

      await shoppingList.removeItem(SHOPPING_LIST_STORAGE_KEY);
      await pending;

      expect(await AsyncStorage.getItem(SHOPPING_LIST_STORAGE_KEY)).toBeNull();
    });

    it('flushPendingShoppingListWrites stores the latest value immediately', async () => {
      const first = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'a');
      const second = shoppingList.setItem(SHOPPING_LIST_STORAGE_KEY, 'b');

      await flushPendingShoppingListWrites();
      await Promise.all([first, second]);

      expect(await AsyncStorage.getItem(SHOPPING_LIST_STORAGE_KEY)).toBe('"b"');
    });
  });

  describe('recipes (not debounced)', () => {
    it('writes immediately without advancing timers', async () => {
      await recipes.setItem(RECIPES_STORAGE_KEY, 'x');

      expect(await AsyncStorage.getItem(RECIPES_STORAGE_KEY)).toBe('"x"');
    });
  });
});
