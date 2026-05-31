import { StyleSheet } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { RecipeListItem } from '@/components/recipe-list-item';
import { TabActionButton, TabActionRow } from '@/components/tab-action-button';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Fonts, RecipesScreenHeader } from '@/constants/theme';
import { useRecipes } from '@/store/recipes';

export default function RecipesScreen() {
  const { recipes, removeRecipe } = useRecipes();

  return (
    <ParallaxScrollView
      headerBackgroundColor={RecipesScreenHeader}
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
      <TabActionRow>
        <TabActionButton label="Create new" href="/recipe/new" />
        <TabActionButton label="Import" href="/recipe/import" />
      </TabActionRow>
      {recipes.length === 0 ? (
        <ThemedView style={styles.emptyState}>
          <ThemedText>No recipes yet.</ThemedText>
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
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  emptyState: {
    gap: 8,
  },
});
