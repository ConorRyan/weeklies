import * as Crypto from 'expo-crypto';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { RECIPES_STORAGE_KEY, recipesStorage } from '@/store/persistence';

export type Ingredient = {
  name: string;
  quantity: string;
};

export type NewRecipe = {
  name: string;
  ingredients: Ingredient[];
};

export type Recipe = {
  id: string;
  name: string;
  ingredients: Ingredient[];
};

type RecipesStore = {
  recipes: Recipe[];
  addRecipe: (recipe: NewRecipe) => void;
  removeRecipe: (id: string) => void;
  updateRecipe: (id: string, recipe: Partial<NewRecipe>) => void;
};

const isIngredient = (value: unknown): value is Ingredient =>
  !!value &&
  typeof value === 'object' &&
  typeof (value as Ingredient).name === 'string' &&
  typeof (value as Ingredient).quantity === 'string';

const isRecipe = (value: unknown): value is Recipe =>
  !!value &&
  typeof value === 'object' &&
  typeof (value as Recipe).id === 'string' &&
  typeof (value as Recipe).name === 'string' &&
  Array.isArray((value as Recipe).ingredients) &&
  (value as Recipe).ingredients.every(isIngredient);

export const useRecipes = create<RecipesStore>()(
  persist(
    (set) => ({
      recipes: [],
      addRecipe: (newRecipe) =>
        set((state) => ({
          recipes: [...state.recipes, { id: Crypto.randomUUID(), ...newRecipe }],
        })),
      removeRecipe: (id) =>
        set((state) => ({
          recipes: state.recipes.filter((recipe) => recipe.id !== id),
        })),
      updateRecipe: (id, recipePatch) =>
        set((state) => ({
          recipes: state.recipes.map((recipe) =>
            recipe.id === id ? { ...recipe, ...recipePatch } : recipe
          ),
        })),
    }),
    {
      name: RECIPES_STORAGE_KEY,
      storage: recipesStorage,
      version: 1,
      partialize: (state) => ({
        recipes: state.recipes,
      }),
      migrate: (persistedState) => {
        const recipes = (persistedState as { recipes?: unknown } | undefined)?.recipes;
        if (!Array.isArray(recipes)) {
          return { recipes: [] };
        }

        return {
          recipes: recipes.filter(isRecipe),
        };
      },
    }
  )
);
