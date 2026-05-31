jest.mock('expo-crypto', () => ({
  randomUUID: jest.fn(() => 'test-uuid-1'),
}));

import { useRecipes } from '@/store/recipes';

describe('useRecipes actions', () => {
  beforeEach(() => {
    useRecipes.setState({ recipes: [] });
  });

  it('adds a recipe with generated id', () => {
    useRecipes.getState().addRecipe({ name: 'Pasta', ingredients: ['salt'] });
    expect(useRecipes.getState().recipes).toEqual([
      { id: 'test-uuid-1', name: 'Pasta', ingredients: ['salt'] },
    ]);
  });

  it('updates a recipe', () => {
    useRecipes.setState({
      recipes: [{ id: '1', name: 'Pasta', ingredients: ['salt'] }],
    });
    useRecipes.getState().updateRecipe('1', { name: 'Updated Pasta' });
    expect(useRecipes.getState().recipes[0].name).toBe('Updated Pasta');
  });

  it('removes a recipe by id', () => {
    useRecipes.setState({
      recipes: [
        { id: '1', name: 'Pasta', ingredients: [] },
        { id: '2', name: 'Soup', ingredients: [] },
      ],
    });
    useRecipes.getState().removeRecipe('1');
    expect(useRecipes.getState().recipes).toHaveLength(1);
    expect(useRecipes.getState().recipes[0].id).toBe('2');
  });
});
