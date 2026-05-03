import { Link, Stack } from 'expo-router';
import { StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';

export default function ModalScreen() {
  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: 'Help' }} />
      <ThemedText>- Create recipes</ThemedText>
      <ThemedText>- Set a recipe for each day</ThemedText>
      <ThemedText>- Auto-populates shopping list</ThemedText>
      <Link href="/(tabs)" dismissTo style={styles.link}>
        <ThemedText type="link">Back</ThemedText>
      </Link>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  link: {
    marginTop: 15,
    paddingVertical: 15,
  },
});
