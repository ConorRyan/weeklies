import { fireEvent, render, screen } from '@testing-library/react-native';
import type { ReactNode } from 'react';

import { SettingsProvider } from '@/contexts/settings';

import { RecipeAddForm } from './recipe-add-form';

const wrapper = ({ children }: { children: ReactNode }) => (
  <SettingsProvider>{children}</SettingsProvider>
);

const onAddRecipe = jest.fn();

const renderForm = async (initialRecipe?: { name: string; ingredients: string[] }) =>
  render(<RecipeAddForm initialRecipe={initialRecipe} onAddRecipe={onAddRecipe} />, { wrapper });

const nameInput = () => screen.getByPlaceholderText('Recipe name');
const ingredientInputs = () => screen.getAllByPlaceholderText('Ingredient');
const save = async () => fireEvent.press(screen.getByText('Save recipe'));
const addIngredient = async () => fireEvent.press(screen.getByText('Add ingredient'));

describe('RecipeAddForm', () => {
  beforeEach(() => {
    onAddRecipe.mockClear();
  });

  it('starts with one empty ingredient row', async () => {
    await renderForm();

    expect(ingredientInputs()).toHaveLength(1);
    expect(nameInput().props.value).toBe('');
  });

  it('requires a recipe name', async () => {
    await renderForm();

    await fireEvent.changeText(ingredientInputs()[0], '200 g pasta');
    await save();

    expect(screen.getByText('Recipe name is required.')).toBeTruthy();
    expect(onAddRecipe).not.toHaveBeenCalled();
  });

  it('requires at least one ingredient', async () => {
    await renderForm();

    await fireEvent.changeText(nameInput(), 'Pasta');
    await save();

    expect(screen.getByText('Add at least one ingredient.')).toBeTruthy();
    expect(onAddRecipe).not.toHaveBeenCalled();
  });

  it('submits a trimmed name with non-empty ingredients and resets', async () => {
    await renderForm();

    await fireEvent.changeText(nameInput(), '  Pasta  ');
    await fireEvent.changeText(ingredientInputs()[0], ' 200 g pasta ');
    await addIngredient();
    await fireEvent.changeText(ingredientInputs()[1], '   ');
    await save();

    expect(onAddRecipe).toHaveBeenCalledTimes(1);
    expect(onAddRecipe).toHaveBeenCalledWith({ name: 'Pasta', ingredients: ['200 g pasta'] });

    expect(nameInput().props.value).toBe('');
    expect(ingredientInputs()).toHaveLength(1);
  });

  it('keeps values when editing an existing recipe', async () => {
    await renderForm({ name: 'Pasta', ingredients: ['200 g pasta'] });

    await save();

    expect(onAddRecipe).toHaveBeenCalledTimes(1);
    expect(nameInput().props.value).toBe('Pasta');
    expect(ingredientInputs()[0].props.value).toBe('200 g pasta');
  });

  it('adds an ingredient row via the add button', async () => {
    await renderForm();

    await addIngredient();

    expect(ingredientInputs()).toHaveLength(2);
  });

  it('removes ingredient rows but keeps at least one', async () => {
    const { getAllByRole, getByRole } = await renderForm();

    await addIngredient();
    expect(ingredientInputs()).toHaveLength(2);

    // Two rows: the '-' button is the only accessibility button in each row.
    const removeButtons = getAllByRole('button');
    await fireEvent.press(removeButtons[0]);
    expect(ingredientInputs()).toHaveLength(1);

    // A single row cannot be removed (button disabled).
    await fireEvent.press(getByRole('button'));
    expect(ingredientInputs()).toHaveLength(1);
  });
});
