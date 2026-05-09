import { DarkTheme, DefaultTheme, ThemeProvider } from '@react-navigation/native';
import { Stack, router } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { AppState, BackHandler, Platform } from 'react-native';
import 'react-native-reanimated';

import { SettingsProvider } from '@/contexts/settings';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { flushPendingShoppingListWrites } from '@/store/persistence';

export const unstable_settings = {
  anchor: '(tabs)',
};

function RootLayoutNav() {
  const colorScheme = useAppColorScheme();

  useEffect(() => {
    const subscription = AppState.addEventListener('change', (nextState) => {
      if (nextState === 'background' || nextState === 'inactive') {
        void flushPendingShoppingListWrites();
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  useEffect(() => {
    if (Platform.OS !== 'android') {
      return;
    }

    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (router.canDismiss()) {
        return false;
      }
      BackHandler.exitApp();
      return true;
    });

    return () => sub.remove();
  }, []);

  return (
    <ThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
      <Stack>
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="help-modal" options={{ presentation: 'modal', title: 'Help' }} />
        <Stack.Screen
          name="recipe/new"
          options={{
            presentation: 'modal',
            headerShown: true,
            title: 'New recipe',
          }}
        />
        <Stack.Screen
          name="recipe/[id]"
          options={{
            presentation: 'modal',
            headerShown: true,
          }}
        />
      </Stack>
      <StatusBar style={colorScheme === 'dark' ? 'light' : 'dark'} />
    </ThemeProvider>
  );
}

export default function RootLayout() {
  return (
    <SettingsProvider>
      <RootLayoutNav />
    </SettingsProvider>
  );
}
