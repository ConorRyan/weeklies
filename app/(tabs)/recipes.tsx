import { Link } from 'expo-router';
import { Pressable, StyleSheet } from 'react-native';
import ParallaxScrollView from '@/components/parallax-scroll-view';
import { RecipeListItem } from '@/components/recipe-list-item';
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
        <ThemedView style={styles.titleLinks}>
          <Link href="/recipe/import" asChild>
            <Pressable>
              <ThemedText type="link">Import with AI</ThemedText>
            </Pressable>
          </Link>
          <Link href="/recipe/new" asChild>
            <Pressable>
              <ThemedText type="link">New recipe</ThemedText>
            </Pressable>
          </Link>
        </ThemedView>
      </ThemedView>
      {recipes.length === 0 ? (
        <ThemedView style={styles.emptyState}>
          <ThemedText>No recipes yet.</ThemedText>
          <Link href="/recipe/new" asChild>
            <Pressable style={styles.emptyLink}>
              <ThemedText type="link">Add a recipe</ThemedText>
            </Pressable>
          </Link>
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
    alignItems: 'center',
    flexDirection: 'row',
    gap: 12,
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  titleLinks: {
    flexDirection: 'row',
    gap: 12,
    flexShrink: 1,
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
  },
  emptyState: {
    gap: 8,
    paddingHorizontal: 16,
  },
  emptyLink: {
    alignSelf: 'flex-start',
  },
});
