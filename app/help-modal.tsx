import { HeaderBackButton } from '@react-navigation/elements';
import { Stack, useRouter } from 'expo-router';
import { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useThemeColor } from '@/hooks/use-theme-color';

export default function HelpModalScreen() {
  const router = useRouter();
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
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled">
        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Weeklies
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Your week is always Monday through Sunday. Tap a day to choose which recipe is on the menu
          that night. The same plan comes back every week until you change it. Pick Clear in the sheet to leave a day
          empty.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Recipes
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Open the Recipes tab to see everything you have saved. Use the New recipe link to add a name and
          ingredient lines (one line per ingredient or amount). Tap a recipe to read it; remove
          it from the list with the minus control when you no longer need it.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Shopping list
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Build your list yourself: type at the bottom and tap + to add a line. Tap a row to mark it
          bought or still needed. Use − to delete a line. Your list is saved on the device.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Settings
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Turn dark mode on or off; the choice is remembered after you close the app. This Help screen
          is linked from Settings.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Data
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Recipes, your weekly plan, and the shopping list are stored locally on this device. They stay
          until you remove them in the app.
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
    padding: 20,
    paddingBottom: 32,
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
