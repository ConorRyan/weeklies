import { HeaderBackButton } from '@react-navigation/elements';
import { Stack, useRouter } from 'expo-router';
import { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useThemeColor } from '@/hooks/use-theme-color';

export default function HelpModalScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
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
          plan across multiple weeks, or change any day whenever your plans change.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Recipes
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Recipes are for meals you already know how to cook. Store the ingredients you need,
          one line at a time, without writing out full cooking instructions. Open the Recipes
          tab to add, review, or remove saved meals.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Shopping list
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          For now, build your list yourself: type at the bottom and tap + to add a line. Soon,
          Weeklies will use your weekly plan to add the right ingredients on the right day for
          the number of portions you need.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Settings
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Turn dark mode on or off; the choice is remembered after you close the app.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Portions per day is the multiplier we will use for recipe quantities when your shopping list is
          built from your weekly plan (coming soon).
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
          app.
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
