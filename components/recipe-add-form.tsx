import { useRef, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';

import type { Ingredient, NewRecipe } from '@/store/recipes';
import { ThemedText } from './themed-text';
import { ThemedTextInput } from './themed-text-input';

type Props = {
  onAddRecipe: (recipe: NewRecipe) => void;
};

const emptyIngredient = (): Ingredient => ({ name: '', quantity: '' });

export function RecipeAddForm({ onAddRecipe }: Props) {
  const [name, setName] = useState('');
  const [ingredients, setIngredients] = useState<Ingredient[]>([emptyIngredient()]);
  const [error, setError] = useState('');
  const ingredientNameRefs = useRef<(TextInput | null)[]>([]);
  const ingredientQtyRefs = useRef<(TextInput | null)[]>([]);

  const updateIngredient = (index: number, patch: Partial<Ingredient>) => {
    setIngredients((state) =>
      state.map((ingredient, i) => (i === index ? { ...ingredient, ...patch } : ingredient))
    );
  };

  const addIngredientRow = (focusNewIngredient = false) => {
    const nextIndex = ingredients.length;
    setIngredients((state) => [...state, emptyIngredient()]);
    if (focusNewIngredient) {
      requestAnimationFrame(() => {
        ingredientNameRefs.current[nextIndex]?.focus();
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
    const cleanedIngredients = ingredients
      .map((ingredient) => ({
        name: ingredient.name.trim(),
        quantity: ingredient.quantity.trim(),
      }))
      .filter((ingredient) => ingredient.name.length > 0 || ingredient.quantity.length > 0);

    const hasInvalidIngredient = cleanedIngredients.some(
      (ingredient) => ingredient.name.length === 0 || ingredient.quantity.length === 0
    );

    if (!trimmedName) {
      setError('Recipe name is required.');
      return;
    }

    if (cleanedIngredients.length === 0 || hasInvalidIngredient) {
      setError('Add at least one ingredient with both name and quantity.');
      return;
    }

    onAddRecipe({ name: trimmedName, ingredients: cleanedIngredients });
    setName('');
    setIngredients([emptyIngredient()]);
    setError('');
  };

  return (
    <View style={styles.container}>
      <ThemedText type="subtitle">Create recipe</ThemedText>
      <ThemedTextInput
        placeholder="Recipe name"
        style={styles.recipeName}
        value={name}
        onChangeText={setName}
        returnKeyType="next"
        onSubmitEditing={() => ingredientNameRefs.current[0]?.focus()}
      />
      {ingredients.map((ingredient, index) => (
        <View key={index} style={styles.ingredientRow}>
          <ThemedTextInput
            placeholder="Ingredient"
            style={styles.ingredientName}
            value={ingredient.name}
            onChangeText={(value) => updateIngredient(index, { name: value })}
            ref={(ref) => {
              ingredientNameRefs.current[index] = ref;
            }}
            returnKeyType="next"
            blurOnSubmit={false}
            onSubmitEditing={() => ingredientQtyRefs.current[index]?.focus()}
          />
          <ThemedTextInput
            placeholder="Qty"
            style={styles.ingredientQty}
            value={ingredient.quantity}
            onChangeText={(value) => updateIngredient(index, { quantity: value })}
            ref={(ref) => {
              ingredientQtyRefs.current[index] = ref;
            }}
            returnKeyType={index === ingredients.length - 1 ? 'done' : 'next'}
            blurOnSubmit={false}
            onSubmitEditing={() => {
              if (index === ingredients.length - 1) {
                addIngredientRow(true);
              } else {
                ingredientNameRefs.current[index + 1]?.focus();
              }
            }}
          />
          <Pressable
            onPress={() => removeIngredientRow(index)}
            disabled={ingredients.length === 1}
            style={styles.smallButton}>
            <ThemedText style={ingredients.length === 1 ? styles.disabled : undefined}>-</ThemedText>
          </Pressable>
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
  ingredientName: {
    borderRadius: 6,
    borderWidth: 1,
    flex: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  ingredientQty: {
    borderRadius: 6,
    borderWidth: 1,
    minWidth: 80,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  smallButton: {
    alignItems: 'center',
    borderRadius: 4,
    borderWidth: 1,
    height: 32,
    justifyContent: 'center',
    width: 32,
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
  disabled: {
    opacity: 0.4,
  },
  error: {
    color: '#d11a2a',
  },
});
