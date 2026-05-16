import { HeaderBackButton } from '@react-navigation/elements';
import { Stack, useRouter } from 'expo-router';
import { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet } from 'react-native';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useDeferredSafeAreaInsets } from '@/hooks/use-deferred-safe-area-insets';
import { useThemeColor } from '@/hooks/use-theme-color';

export default function HelpModalScreen() {
  const router = useRouter();
  const insets = useDeferredSafeAreaInsets();
  const tintColor = useThemeColor({}, 'tint');

  const handleBack = useCallback(() => {
    if (router.canGoBack()) {
      router.back();
    } else {
      router.dismissTo('/(tabs)/settings');
    }
  }, [router]);

  return (
    <ThemedView style={styles.screen}>
      <Stack.Screen
        options={{
          title: 'Help',
          headerLeft: (props) => (
            <HeaderBackButton
              {...props}
              onPress={handleBack}
              tintColor={props.tintColor ?? tintColor}
            />
          ),
        }}
      />
      <ScrollView
        contentContainerStyle={[
          styles.scrollContent,
          { paddingBottom: insets.bottom + 40 },
        ]}
        keyboardShouldPersistTaps="handled">
        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Weeklies
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Weeklies is for planning meals once, then not asking yourself what to eat every day.
          Your week runs Monday through Sunday. Tap a day to choose a recipe, reuse the same
          plan across multiple weeks, or change any day whenever your plans change. Today’s row
          has a light highlight and the day name and recipe line are bold so you can spot the
          current day quickly.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Recipes
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Recipes are for meals you already know how to cook. Store the ingredients you need,
          one line at a time, without writing out full cooking instructions. Open the Recipes
          tab to add, review, or remove saved meals.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          To edit a recipe, open it from the list and tap Edit recipe. The screen matches new
          recipe (same fields and Save recipe), with your current name and ingredients filled
          in; saving replaces that recipe and keeps the same entry everywhere it is used in your
          plan.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Shopping list
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Type at the bottom and tap + to add a line. Tap the checkbox or anywhere on the row to
          check an item off. Use Remove checked to delete every checked line.
          Tap Add today&apos;s recipe to append ingredients from whatever recipe is planned for
          today on the Weeklies tab; amounts are scaled using Portions per day in Settings (each
          number in an ingredient line is multiplied by that value).
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Settings
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Turn dark mode on or off; the choice is remembered after you close the app.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Portions per day is the multiplier used when you use Add today&apos;s recipe on the
          shopping list: every numeric quantity in each ingredient line is multiplied by this
          value.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          This Help screen is linked from Settings.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Data
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Weeklies is fully local and completely offline. Recipes, your weekly plan, and the
          shopping list are stored on this device, where they stay until you remove them in the
          app or replace them by importing a backup.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          In Settings, open the Data section.{' '}
          <ThemedText type="defaultSemiBold">Export</ThemedText>
          {' '}
          writes one JSON file (default name like backup-2026-05-09.json) containing recipes,
          your weekly plan, the shopping list, and settings—including dark mode and portions per
          day.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          <ThemedText type="defaultSemiBold">Import</ThemedText>
          {' '}
          replaces all stored data with the contents of that file. Nothing is merged with what you
          already had—confirm only when you intend to overwrite the app completely.
        </ThemedText>

        <Pressable onPress={handleBack} style={styles.footerLink} accessibilityRole="button">
          <ThemedText type="link">Back</ThemedText>
        </Pressable>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 20,
  },
  sectionTitle: {
    marginTop: 20,
    marginBottom: 8,
  },
  paragraph: {
    lineHeight: 22,
  },
  footerLink: {
    marginTop: 28,
    paddingVertical: 12,
    alignSelf: 'flex-start',
  },
});
