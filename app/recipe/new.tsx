import { Stack, router } from 'expo-router';
import { StyleSheet } from 'react-native';

import { RecipeAddForm } from '@/components/recipe-add-form';
import { ThemedView } from '@/components/themed-view';
import { useRecipes } from '@/store/recipes';

export default function NewRecipeScreen() {
  const addRecipe = useRecipes((s) => s.addRecipe);

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: 'New recipe' }} />
      <RecipeAddForm
        onAddRecipe={(recipe) => {
          addRecipe(recipe);
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
