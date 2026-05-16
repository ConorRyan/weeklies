import { Link } from 'expo-router';
import { Pressable, StyleSheet, View } from 'react-native';

import { RecipesScreenHeader } from '@/constants/theme';
import type { Recipe } from '@/store/recipes';
import { confirmAction } from '@/utils/confirm';
import { ListSquareActionButton } from './list-square-action-button';
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
      <ListSquareActionButton
        accent={RecipesScreenHeader}
        label="-"
        onPress={() =>
          confirmAction({
            title: 'Delete recipe?',
            message: `Remove "${recipe.name}"? This cannot be undone.`,
            confirmLabel: 'Delete',
            destructive: true,
            onConfirm: () => onRemove(recipe.id),
          })
        }
      />
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
});
