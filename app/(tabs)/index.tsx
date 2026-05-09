import { useMemo, useState } from 'react';
import { FlatList, Modal, Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors, Fonts } from '@/constants/theme';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { useRecipes } from '@/store/recipes';
import {
  useWeeklyPlan,
  WEEKDAY_LABELS,
  WEEKDAYS,
  weekdayFromDate,
  type Weekday,
} from '@/store/weekly-plan';
import { Link } from 'expo-router';

export default function WeekliesScreen() {
  const colorScheme = useAppColorScheme();
  const insets = useSafeAreaInsets();
  const { recipes } = useRecipes();
  const { byDay, setDayRecipe } = useWeeklyPlan();
  const [pickerDay, setPickerDay] = useState<Weekday | null>(null);

  const recipeById = useMemo(() => {
    const m = new Map<string, (typeof recipes)[0]>();
    for (const r of recipes) {
      m.set(r.id, r);
    }
    return m;
  }, [recipes]);

  const modalTint = Colors[colorScheme ?? 'light'].tint;

  const closePicker = () => setPickerDay(null);

  const handlePickRecipe = (recipeId: string) => {
    if (pickerDay) {
      setDayRecipe(pickerDay, recipeId);
    }
    closePicker();
  };

  const handleClearDay = () => {
    if (pickerDay) {
      setDayRecipe(pickerDay, null);
    }
    closePicker();
  };

  const todayWeekday = weekdayFromDate(new Date());

  return (
    <>
      <ParallaxScrollView
        headerBackgroundColor={{ light: '#1DA1FF', dark: '#0F6FC1' }}
        headerImage={
          <IconSymbol
            size={310}
            color="#FFFFFF"
            name="calendar"
            style={styles.headerImage}
          />
        }>
        <ThemedView style={styles.titleContainer}>
          <ThemedText
            type="title"
            style={{
              fontFamily: Fonts.rounded,
            }}>
            Weeklies
          </ThemedText>
        </ThemedView>
        <ThemedText style={styles.subtitle}>
          Tap a row to assign a recipe. Repeats every week.
        </ThemedText>
        <View>
          {WEEKDAYS.map((day) => {
            const recipeId = byDay[day];
            const recipe = recipeId ? recipeById.get(recipeId) : undefined;
            let line: string;
            if (!recipeId) {
              line = 'Choose recipe';
            } else if (!recipe) {
              line = 'Recipe removed';
            } else {
              line = recipe.name;
            }

            const isToday = day === todayWeekday;

            return (
              <Pressable
                key={day}
                onPress={() => setPickerDay(day)}
                style={({ pressed }) => [
                  styles.dayRow,
                  isToday && styles.dayRowToday,
                  pressed && styles.dayRowPressed,
                ]}>
                <ThemedText
                  type="default"
                  style={[styles.dayRowLabel, isToday && styles.dayRowTextToday]}
                  numberOfLines={1}>
                  {WEEKDAY_LABELS[day]}
                </ThemedText>
                <View style={styles.dayRowValueWrap}>
                  <ThemedText
                    numberOfLines={1}
                    ellipsizeMode="tail"
                    style={[
                      styles.dayRecipe,
                      (!recipeId || !recipe) && styles.dayRecipeMuted,
                      isToday && styles.dayRowTextToday,
                    ]}>
                    {line}
                  </ThemedText>
                </View>
              </Pressable>
            );
          })}
        </View>
      </ParallaxScrollView>

      <Modal
        visible={pickerDay !== null}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={closePicker}>
        <View
          style={[
            styles.modalContainer,
            {
              backgroundColor: Colors[colorScheme ?? 'light'].background,
              paddingTop: insets.top + 16,
              paddingBottom: insets.bottom,
            },
          ]}>
          <View style={styles.modalHeader}>
            <ThemedText type="subtitle">
              {pickerDay ? WEEKDAY_LABELS[pickerDay] : ''}
            </ThemedText>
            <Pressable onPress={closePicker} hitSlop={12}>
              <ThemedText type="defaultSemiBold" style={{ color: modalTint }}>
                Done
              </ThemedText>
            </Pressable>
          </View>
          {pickerDay && byDay[pickerDay] ? (
            <Pressable onPress={handleClearDay} style={styles.clearButton}>
              <ThemedText style={{ color: modalTint }}>Clear assignment</ThemedText>
            </Pressable>
          ) : null}
          {recipes.length === 0 ? (
            <ThemedView style={styles.emptyPicker}>
              <ThemedText>Add recipes first, then pick one for each day.</ThemedText>
              <Link href="/(tabs)/recipes" asChild>
                <Pressable>
                  <ThemedText type="defaultSemiBold" style={{ color: modalTint }}>
                    Go to Recipes
                  </ThemedText>
                </Pressable>
              </Link>
            </ThemedView>
          ) : (
            <FlatList
              data={recipes}
              keyExtractor={(item) => item.id}
              contentContainerStyle={styles.listContent}
              renderItem={({ item }) => (
                <Pressable
                  onPress={() => handlePickRecipe(item.id)}
                  style={({ pressed }) => [
                    styles.recipeRow,
                    pressed && styles.recipeRowPressed,
                  ]}>
                  <ThemedText type="defaultSemiBold">{item.name}</ThemedText>
                  <ThemedText style={styles.recipeMeta}>
                    {item.ingredients.length} ingredient{item.ingredients.length === 1 ? '' : 's'}
                  </ThemedText>
                </Pressable>
              )}
            />
          )}
        </View>
      </Modal>
    </>
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
    alignItems: 'center',
    gap: 8,
  },
  subtitle: {
    opacity: 0.75,
    marginBottom: 4,
    paddingHorizontal: 16,
  },
  dayRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(128,128,128,0.35)',
  },
  dayRowPressed: {
    opacity: 0.85,
  },
  dayRowToday: {
    backgroundColor: 'rgba(29, 161, 255, 0.14)',
  },
  dayRowTextToday: {
    fontWeight: '700',
  },
  dayRowLabel: {
    flexShrink: 0,
  },
  dayRowValueWrap: {
    flex: 1,
    minWidth: 0,
    alignItems: 'flex-end',
  },
  dayRecipe: {
    fontSize: 16,
    textAlign: 'right',
    width: '100%',
  },
  dayRecipeMuted: {
    opacity: 0.55,
  },
  modalContainer: {
    flex: 1,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingBottom: 12,
  },
  clearButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  emptyPicker: {
    padding: 24,
    gap: 12,
    alignItems: 'flex-start',
  },
  listContent: {
    paddingBottom: 24,
  },
  recipeRow: {
    gap: 4,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(128,128,128,0.35)',
  },
  recipeRowPressed: {
    opacity: 0.85,
  },
  recipeMeta: {
    opacity: 0.7,
    fontSize: 14,
  },
});
