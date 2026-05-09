import { useRef, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';

import { RecipesScreenHeader } from '@/constants/theme';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import type { NewRecipe } from '@/store/recipes';
import { ListSquareActionButton } from './list-square-action-button';
import { ThemedText } from './themed-text';
import { ThemedTextInput } from './themed-text-input';

type Props = {
  onAddRecipe: (recipe: NewRecipe) => void;
};

export function RecipeAddForm({ onAddRecipe }: Props) {
  const colorScheme = useAppColorScheme();
  const inputBorderColor = colorScheme === 'dark' ? '#FFFFFF' : '#9A9A9A';

  const [name, setName] = useState('');
  const [ingredients, setIngredients] = useState<string[]>(['']);
  const [error, setError] = useState('');
  const ingredientRefs = useRef<(TextInput | null)[]>([]);

  const updateIngredientLine = (index: number, value: string) => {
    setIngredients((state) => state.map((line, i) => (i === index ? value : line)));
  };

  const addIngredientRow = (focusNewIngredient = false) => {
    const nextIndex = ingredients.length;
    setIngredients((state) => [...state, '']);
    if (focusNewIngredient) {
      requestAnimationFrame(() => {
        ingredientRefs.current[nextIndex]?.focus();
      });
    }
  };

  const removeIngredientRow = (index: number) => {
    setIngredients((state) => {
      if (state.length === 1) {
        return state;
      }
      return state.filter((_, i) => i !== index);
    });
  };

  const submit = () => {
    const trimmedName = name.trim();
    const cleanedIngredients = ingredients.map((line) => line.trim()).filter((line) => line.length > 0);

    if (!trimmedName) {
      setError('Recipe name is required.');
      return;
    }

    if (cleanedIngredients.length === 0) {
      setError('Add at least one ingredient.');
      return;
    }

    onAddRecipe({ name: trimmedName, ingredients: cleanedIngredients });
    setName('');
    setIngredients(['']);
    setError('');
  };

  return (
    <View style={styles.container}>
      <ThemedText style={styles.fieldHeading}>Name</ThemedText>
      <ThemedTextInput
        placeholder="Recipe name"
        style={[styles.recipeName, { borderColor: inputBorderColor }]}
        value={name}
        onChangeText={setName}
        returnKeyType="next"
        onSubmitEditing={() => ingredientRefs.current[0]?.focus()}
      />
      <ThemedText style={styles.fieldHeading}>Ingredients</ThemedText>
      {ingredients.map((line, index) => (
        <View key={index} style={styles.ingredientRow}>
          <ThemedTextInput
            placeholder="Ingredient"
            style={[styles.ingredientLine, { borderColor: inputBorderColor }]}
            value={line}
            onChangeText={(value) => updateIngredientLine(index, value)}
            ref={(ref) => {
              ingredientRefs.current[index] = ref;
            }}
            returnKeyType={index === ingredients.length - 1 ? 'done' : 'next'}
            blurOnSubmit={false}
            onSubmitEditing={() => {
              if (index === ingredients.length - 1) {
                addIngredientRow(true);
              } else {
                ingredientRefs.current[index + 1]?.focus();
              }
            }}
          />
          <ListSquareActionButton
            accent={RecipesScreenHeader}
            disabled={ingredients.length === 1}
            label="-"
            onPress={() => removeIngredientRow(index)}
          />
        </View>
      ))}
      <View style={styles.actions}>
        <Pressable onPress={() => addIngredientRow()} style={styles.actionButton}>
          <ThemedText type="defaultSemiBold">Add ingredient</ThemedText>
        </Pressable>
        <Pressable onPress={submit} style={styles.actionButton}>
          <ThemedText type="defaultSemiBold">Save recipe</ThemedText>
        </Pressable>
      </View>
      {error ? <ThemedText style={styles.error}>{error}</ThemedText> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: 10,
    marginBottom: 12,
    paddingHorizontal: 16,
  },
  fieldHeading: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: -4,
    opacity: 0.9,
  },
  recipeName: {
    borderRadius: 6,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  ingredientRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
  },
  ingredientLine: {
    borderRadius: 6,
    borderWidth: 1,
    flex: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  actionButton: {
    borderRadius: 6,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  error: {
    color: '#d11a2a',
  },
});
