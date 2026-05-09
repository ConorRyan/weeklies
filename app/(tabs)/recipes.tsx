import { StyleSheet } from 'react-native';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { RecipeAddForm } from '@/components/recipe-add-form';
import { RecipeListItem } from '@/components/recipe-list-item';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Fonts } from '@/constants/theme';
import { useRecipes } from '@/store/recipes';

export default function RecipesScreen() {
  const { recipes, addRecipe, removeRecipe } = useRecipes();

  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#FF4D4D', dark: '#8E0000' }}
      headerImage={
        <IconSymbol
          size={310}
          color="#FFFFFF"
          name="book"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText
          type="title"
          style={{
            fontFamily: Fonts.rounded,
          }}>
          Recipes
        </ThemedText>
      </ThemedView>
      <RecipeAddForm onAddRecipe={addRecipe} />
      {recipes.length === 0 ? (
        <ThemedView style={styles.emptyState}>
          <ThemedText>No recipes yet. Create one above.</ThemedText>
        </ThemedView>
      ) : (
        recipes.map((recipe) => (
          <RecipeListItem key={recipe.id} recipe={recipe} onRemove={removeRecipe} />
        ))
      )}
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    color: '#FFFFFF',
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  emptyState: {
    paddingHorizontal: 16,
  },
});
