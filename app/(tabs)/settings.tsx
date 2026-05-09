import { Link } from 'expo-router';
import { Pressable, StyleSheet, Switch } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors, Fonts, SettingsScreenHeader } from '@/constants/theme';
import { useThemePreference } from '@/contexts/theme-preference';
import { useColorScheme } from '@/hooks/use-color-scheme';

export default function SettingsScreen() {
  const colorScheme = useColorScheme();
  const { setColorScheme } = useThemePreference();
  const tint = Colors[colorScheme].tint;

  return (
    <ParallaxScrollView
      headerBackgroundColor={SettingsScreenHeader}
      headerImage={
        <IconSymbol
          size={310}
          color="#FFFFFF"
          name="gearshape"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText
          type="title"
          style={{
            fontFamily: Fonts.rounded,
          }}>
          Settings
        </ThemedText>
      </ThemedView>
      <ThemedView style={styles.row}>
        <ThemedText style={styles.label}>Dark mode</ThemedText>
        <Switch
          value={colorScheme === 'dark'}
          onValueChange={(on) => setColorScheme(on ? 'dark' : 'light')}
          trackColor={{ false: '#767577', true: '#81b0ff' }}
          thumbColor={colorScheme === 'dark' ? tint : '#f4f3f4'}
        />
      </ThemedView>
      <ThemedView style={styles.helpLink}>
        <Link href="/help-modal" asChild>
          <Pressable>
            <ThemedText type="link">Help</ThemedText>
          </Pressable>
        </Link>
      </ThemedView>
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
    alignItems: 'center',
    gap: 8,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 24,
    paddingVertical: 8,
  },
  label: {
    fontSize: 17,
  },
  helpLink: {
    marginTop: 8,
    paddingVertical: 8,
  },
});
