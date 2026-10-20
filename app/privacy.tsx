import { HeaderBackButton } from '@react-navigation/elements';
import { Stack, useRouter } from 'expo-router';
import { useCallback } from 'react';
import { Pressable, ScrollView, StyleSheet } from 'react-native';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { useDeferredSafeAreaInsets } from '@/hooks/use-deferred-safe-area-insets';
import { useThemeColor } from '@/hooks/use-theme-color';

export default function PrivacyScreen() {
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
          title: 'Privacy policy',
          headerLeft: (props) => (
            <HeaderBackButton
              {...props}
              onPress={handleBack}
              tintColor={typeof props.tintColor === 'string' ? props.tintColor : tintColor}
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
          Summary
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Weeklies stores your data on your device. The app has no accounts, no analytics, no
          advertising, and no tracking. It does not send your data anywhere unless you use the
          optional AI recipe import described below.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Data stored on your device
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Your recipes, weekly plan, shopping list, and settings (dark mode, portions per day)
          are saved in local storage on this device only. They stay there until you delete them
          in the app or replace them by importing a backup file. This data is never transmitted
          to us or to any server.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Backups
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Export (Settings → Data) writes a JSON file to a location you choose. The file is
          yours: the app never uploads it, and the app has no access to any other files on your
          device. Import reads only the backup file you pick, and replaces your stored data with
          its contents.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Recipe import
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          <ThemedText type="defaultSemiBold">Import</ThemedText>
          {' '}
          fetches the recipe page from the web address you enter, on your device, and reads the
          recipe data from the page itself. The request goes directly to that website, the same
          as opening it in a browser.
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          <ThemedText type="defaultSemiBold">Import with AI</ThemedText>
          {' '}
          is optional and only runs when you ask for it. It sends the recipe web address or page
          text you provide to the Mistral AI API (api.mistral.ai) using your own API key, so the
          recipe can be extracted when the page does not contain structured recipe data. Your
          API key is stored on this device only and is never included in backup exports. These
          requests are processed by Mistral according to Mistral&apos;s privacy policy; we do not
          see them and cannot access your key. Without this feature, the app makes no network
          requests at all.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Permissions
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          On Android, the app can open the system file picker when you export or import a
          backup, and only for the file or folder you select. It requests no other permissions.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Children
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Weeklies is a general-purpose meal planner and is not directed at children. Because
          the app collects no personal data, no age verification is used.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Changes to this policy
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          If the app changes in a way that affects your privacy, this policy will be updated
          and published with the matching app update. The current version is always available
          at weeklies.expo.app/privacy and inside the app.
        </ThemedText>

        <ThemedText type="subtitle" style={styles.sectionTitle}>
          Contact
        </ThemedText>
        <ThemedText style={styles.paragraph}>
          Questions about this policy can be raised on GitHub:
          github.com/ConorRyan/weeklies.
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
