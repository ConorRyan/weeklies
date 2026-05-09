import { Stack, router, useLocalSearchParams } from 'expo-router';
import { StyleSheet } from 'react-native';

import { RecipeAddForm } from '@/components/recipe-add-form';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useRecipes } from '@/store/recipes';

export default function EditRecipeScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const recipe = useRecipes((state) => state.recipes.find((item) => item.id === id));
  const updateRecipe = useRecipes((s) => s.updateRecipe);

  if (!recipe) {
    return (
      <ThemedView style={styles.container}>
        <Stack.Screen options={{ title: 'Edit recipe' }} />
        <ThemedText type="title">Recipe not found</ThemedText>
        <ThemedText>The recipe may have been deleted.</ThemedText>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: 'New recipe' }} />
      <RecipeAddForm
        initialRecipe={{ name: recipe.name, ingredients: recipe.ingredients }}
        onAddRecipe={(next) => {
          updateRecipe(recipe.id, next);
          router.back();
        }}
      />
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 12,
  },
});
