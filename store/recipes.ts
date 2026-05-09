import * as Crypto from 'expo-crypto';
import { create } from 'zustand';

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

export const useRecipes = create<RecipesStore>((set) => ({
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
}));
