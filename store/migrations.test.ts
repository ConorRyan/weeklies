import AsyncStorage from '@react-native-async-storage/async-storage';

import {
  RECIPES_STORAGE_KEY,
  SHOPPING_LIST_STORAGE_KEY,
  WEEKLY_PLAN_STORAGE_KEY,
} from '@/store/persistence';
import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { emptyByDay, useWeeklyPlan } from '@/store/weekly-plan';

async function seed(key: string, state: unknown, version: number) {
  await AsyncStorage.setItem(key, JSON.stringify({ state, version }));
}

describe('store migrations', () => {
  beforeEach(async () => {
    await AsyncStorage.clear();
  });

  describe('recipes', () => {
    it('migrates legacy ingredient objects and drops invalid recipes', async () => {
      await seed(
        RECIPES_STORAGE_KEY,
        {
          recipes: [
            { id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] },
            { id: 'r2', name: 'Soup', ingredients: [{ quantity: '400 g', name: 'lentils' }] },
            { id: 3, name: 'Broken', ingredients: [] },
            { id: 'r4', name: 'Empty', ingredients: ['', '   '] },
          ],
        },
        1
      );

      await useRecipes.persist.rehydrate();

      // Recipes with only whitespace ingredient lines survive with an
      // empty ingredient list (documented normalizeRecipe behavior).
      expect(useRecipes.getState().recipes).toEqual([
        { id: 'r1', name: 'Pasta', ingredients: ['200 g pasta'] },
        { id: 'r2', name: 'Soup', ingredients: ['400 g lentils'] },
        { id: 'r4', name: 'Empty', ingredients: [] },
      ]);
    });

    it('resets to empty for corrupt persisted state', async () => {
      await seed(RECIPES_STORAGE_KEY, { recipes: 'not an array' }, 1);

      await useRecipes.persist.rehydrate();

      expect(useRecipes.getState().recipes).toEqual([]);
    });
  });

  describe('shopping list', () => {
    it('migrates legacy name/quantity items and drops invalid ones', async () => {
      await seed(
        SHOPPING_LIST_STORAGE_KEY,
        {
          items: [
            { id: 'i1', label: 'Milk', checked: true },
            { id: 'i2', name: 'flour', quantity: '500 g', checked: false },
            { id: 'i3', name: 'salt', quantity: '   ', checked: false },
            { id: 4, label: 'Broken', checked: false },
          ],
        },
        1
      );

      await useShoppingList.persist.rehydrate();

      // A whitespace-only quantity degrades to the bare name.
      expect(useShoppingList.getState().items).toEqual([
        { id: 'i1', label: 'Milk', checked: true },
        { id: 'i2', label: '500 g flour', checked: false },
        { id: 'i3', label: 'salt', checked: false },
      ]);
    });

    it('resets to empty for corrupt persisted state', async () => {
      await seed(SHOPPING_LIST_STORAGE_KEY, {}, 1);

      await useShoppingList.persist.rehydrate();

      expect(useShoppingList.getState().items).toEqual([]);
    });
  });

  describe('weekly plan', () => {
    it('keeps only valid weekdays with string recipe ids', async () => {
      await seed(
        WEEKLY_PLAN_STORAGE_KEY,
        {
          byDay: {
            mon: 'r1',
            someday: 'r2',
            wed: 42,
            thu: null,
            fri: undefined,
          },
        },
        0
      );

      await useWeeklyPlan.persist.rehydrate();

      expect(useWeeklyPlan.getState().byDay).toEqual({
        ...emptyByDay(),
        mon: 'r1',
      });
    });

    it('resets to an empty plan for corrupt persisted state', async () => {
      await seed(WEEKLY_PLAN_STORAGE_KEY, { byDay: 'nope' }, 0);

      await useWeeklyPlan.persist.rehydrate();

      expect(useWeeklyPlan.getState().byDay).toEqual(emptyByDay());
    });
  });
});
