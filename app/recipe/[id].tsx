import { Link, Stack, useLocalSearchParams } from 'expo-router';
import { StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useRecipes } from '@/store/recipes';

export default function RecipeDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const recipe = useRecipes((state) => state.recipes.find((item) => item.id === id));

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: recipe?.name ?? 'Recipe' }} />
      {recipe ? (
        <>
          <ThemedText type="title">{recipe.name}</ThemedText>
          <ThemedText type="subtitle">Ingredients (single portion)</ThemedText>
          <ThemedView style={styles.ingredients}>
            {recipe.ingredients.map((line, index) => (
              <ThemedText key={`${line}-${index}`}>- {line}</ThemedText>
            ))}
          </ThemedView>
        </>
      ) : (
        <>
          <ThemedText type="title">Recipe not found</ThemedText>
          <ThemedText>The recipe may have been deleted.</ThemedText>
        </>
      )}
      <Link href="/(tabs)/recipes" style={styles.link}>
        <ThemedText type="link">Back to recipes</ThemedText>
      </Link>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: 12,
    padding: 16,
  },
  ingredients: {
    gap: 6,
  },
  link: {
    marginTop: 8,
  },
});
