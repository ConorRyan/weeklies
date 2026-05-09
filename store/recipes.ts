import * as Crypto from 'expo-crypto';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

import { RECIPES_STORAGE_KEY, recipesStorage } from '@/store/persistence';

export type NewRecipe = {
  name: string;
  ingredients: string[];
};

export type Recipe = {
  id: string;
  name: string;
  ingredients: string[];
};

type RecipesStore = {
  recipes: Recipe[];
  addRecipe: (recipe: NewRecipe) => void;
  removeRecipe: (id: string) => void;
  updateRecipe: (id: string, recipe: Partial<NewRecipe>) => void;
};

const ingredientToLine = (value: unknown): string | null => {
  if (typeof value === 'string') {
    const t = value.trim();
    return t.length > 0 ? t : null;
  }
  if (value && typeof value === 'object' && 'name' in value && 'quantity' in value) {
    const name = String((value as { name: unknown }).name).trim();
    const quantity = String((value as { quantity: unknown }).quantity).trim();
    const line = [quantity, name].filter((s) => s.length > 0).join(' ');
    return line.length > 0 ? line : null;
  }
  return null;
};

const normalizeRecipe = (value: unknown): Recipe | null => {
  if (!value || typeof value !== 'object') {
    return null;
  }
  const r = value as Record<string, unknown>;
  if (typeof r.id !== 'string' || typeof r.name !== 'string' || !Array.isArray(r.ingredients)) {
    return null;
  }
  const ingredients = r.ingredients
    .map(ingredientToLine)
    .filter((line): line is string => line !== null);
  return { id: r.id, name: r.name, ingredients };
};

const isRecipe = (value: unknown): value is Recipe =>
  !!value &&
  typeof value === 'object' &&
  typeof (value as Recipe).id === 'string' &&
  typeof (value as Recipe).name === 'string' &&
  Array.isArray((value as Recipe).ingredients) &&
  (value as Recipe).ingredients.every((i) => typeof i === 'string');

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
      version: 2,
      partialize: (state) => ({
        recipes: state.recipes,
      }),
      migrate: (persistedState) => {
        const recipes = (persistedState as { recipes?: unknown } | undefined)?.recipes;
        if (!Array.isArray(recipes)) {
          return { recipes: [] };
        }

        return {
          recipes: recipes
            .map(normalizeRecipe)
            .filter((recipe): recipe is Recipe => recipe !== null)
            .filter(isRecipe),
        };
      },
    }
  )
);
