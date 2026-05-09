import { Link } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import type { Recipe } from '@/store/recipes';
import { ThemedText } from './themed-text';

type Props = {
  recipe: Recipe;
  onRemove: (id: string) => void;
};

export function RecipeListItem({ recipe, onRemove }: Props) {
  return (
    <View style={styles.row}>
      <Link href={`/recipe/${recipe.id}`} asChild>
        <Pressable style={styles.mainButton}>
          <ThemedText type="defaultSemiBold">{recipe.name}</ThemedText>
          <ThemedText style={styles.subtitle}>
            {recipe.ingredients.length} ingredient{recipe.ingredients.length === 1 ? '' : 's'}
          </ThemedText>
        </Pressable>
      </Link>
      <Pressable onPress={() => onRemove(recipe.id)} style={styles.removeButton}>
        <ThemedText>-</ThemedText>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  mainButton: {
    flex: 1,
    gap: 4,
    paddingVertical: 8,
  },
  subtitle: {
    opacity: 0.7,
  },
  removeButton: {
    alignItems: 'center',
    borderRadius: 4,
    borderWidth: 1,
    height: 28,
    justifyContent: 'center',
    width: 28,
  },
});
