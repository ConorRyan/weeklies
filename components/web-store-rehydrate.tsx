import { useEffect } from 'react';
import { Platform } from 'react-native';

import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { useWeeklyPlan } from '@/store/weekly-plan';

/** Loads persisted Zustand state after hydration on web (pairs with skipHydration in store config). */
export function WebStoreRehydrate() {
  useEffect(() => {
    if (Platform.OS !== 'web') {
      return;
    }
    void Promise.all([
      useRecipes.persist.rehydrate(),
      useShoppingList.persist.rehydrate(),
      useWeeklyPlan.persist.rehydrate(),
    ]);
  }, []);

  return null;
}
