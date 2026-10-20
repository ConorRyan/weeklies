import { render } from '@testing-library/react-native';
import { Platform } from 'react-native';

import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { useWeeklyPlan } from '@/store/weekly-plan';

import { WebStoreRehydrate } from './web-store-rehydrate';

const setOS = (os: string) => {
  Object.defineProperty(Platform, 'OS', { value: os, configurable: true, writable: true });
};

describe('WebStoreRehydrate', () => {
  afterEach(() => {
    setOS('ios');
    jest.restoreAllMocks();
  });

  it('rehydrates all stores on web', async () => {
    setOS('web');
    const spies = [
      jest.spyOn(useRecipes.persist, 'rehydrate'),
      jest.spyOn(useShoppingList.persist, 'rehydrate'),
      jest.spyOn(useWeeklyPlan.persist, 'rehydrate'),
    ];

    await render(<WebStoreRehydrate />);

    for (const spy of spies) {
      expect(spy).toHaveBeenCalledTimes(1);
    }
  });

  it('does nothing on native platforms', async () => {
    setOS('ios');
    const spy = jest.spyOn(useRecipes.persist, 'rehydrate');

    await render(<WebStoreRehydrate />);

    expect(spy).not.toHaveBeenCalled();
  });
});
