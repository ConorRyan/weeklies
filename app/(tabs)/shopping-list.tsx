import { useMemo } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ShoppingListAddLine } from '@/components/shopping-list-add-line';
import { ShoppingListItem } from '@/components/shopping-list-item';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors, Fonts, ShoppingListScreenHeader } from '@/constants/theme';
import { usePortionsPerDay } from '@/contexts/settings';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { useRecipes } from '@/store/recipes';
import { useShoppingList } from '@/store/shopping-list';
import { useWeeklyPlan, weekdayFromDate } from '@/store/weekly-plan';
import { notifyInfo } from '@/utils/notify';
import { scaleIngredientLine } from '@/utils/scale-ingredient-line';

export default function ShoppingListScreen() {
  const colorScheme = useAppColorScheme();
  const { portionsPerDay } = usePortionsPerDay();
  const { recipes } = useRecipes();
  const { byDay } = useWeeklyPlan();
  const { items, toggleItem, addItem, removeCheckedItems } = useShoppingList();

  const tint = Colors[colorScheme ?? 'light'].tint;
  const hasChecked = useMemo(() => items.some((i) => i.checked), [items]);

  const handleAddTodaysRecipe = (): void => {
    const day = weekdayFromDate(new Date());
    const recipeId = byDay[day];
    if (!recipeId) {
      notifyInfo(
        'No recipe today',
        'Assign a recipe to today on the Weeklies tab first.'
      );
      return;
    }
    const recipe = recipes.find((r) => r.id === recipeId);
    if (!recipe) {
      notifyInfo(
        'Recipe missing',
        'The planned recipe was removed. Update Weeklies or Recipes.'
      );
      return;
    }
    const factor = portionsPerDay;
    for (const line of recipe.ingredients) {
      const trimmed = line.trim();
      if (trimmed.length === 0) {
        continue;
      }
      addItem({ label: scaleIngredientLine(trimmed, factor) });
    }
  };

  return (
    <ParallaxScrollView
      headerBackgroundColor={ShoppingListScreenHeader}
      headerImage={
        <IconSymbol
          size={310}
          color="#FFFFFF"
          name="cart"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText
          type="title"
          style={{
            fontFamily: Fonts.rounded,
          }}>
          Shopping List
        </ThemedText>
      </ThemedView>
      <View style={styles.actionRow}>
        <Pressable
          onPress={handleAddTodaysRecipe}
          style={({ pressed }) => [
            styles.actionButton,
            styles.actionButtonEqual,
            { borderColor: tint },
            pressed && styles.actionButtonPressed,
          ]}>
          <ThemedText
            type="defaultSemiBold"
            style={{ color: tint, textAlign: 'center' }}
            numberOfLines={2}>
            Add today&apos;s recipe
          </ThemedText>
        </Pressable>
        <Pressable
          disabled={!hasChecked}
          onPress={removeCheckedItems}
          style={({ pressed }) => [
            styles.actionButton,
            styles.actionButtonEqual,
            styles.removeCheckedButton,
            !hasChecked && styles.actionButtonDisabled,
            pressed && hasChecked && styles.actionButtonPressed,
          ]}>
          <ThemedText
            type="defaultSemiBold"
            style={styles.removeCheckedButtonLabel}
            numberOfLines={2}>
            Remove checked
          </ThemedText>
        </Pressable>
      </View>
      {items.map((item) => (
        <ShoppingListItem
          key={item.id}
          {...item}
          onToggle={() => toggleItem(item.id)}
        />
      ))}
      <ShoppingListAddLine></ShoppingListAddLine>
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
  actionRow: {
    flexDirection: 'row',
    alignItems: 'stretch',
    gap: 8,
    marginHorizontal: 16,
    marginBottom: 8,
  },
  actionButton: {
    paddingVertical: 10,
    paddingHorizontal: 8,
    borderRadius: 10,
    borderWidth: StyleSheet.hairlineWidth * 2,
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 44,
  },
  actionButtonEqual: {
    flex: 1,
    minWidth: 0,
  },
  actionButtonPressed: {
    opacity: 0.85,
  },
  actionButtonDisabled: {
    opacity: 0.45,
  },
  removeCheckedButton: {
    borderColor: '#C62828',
  },
  removeCheckedButtonLabel: {
    color: '#C62828',
    textAlign: 'center',
  },
});
