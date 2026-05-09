import { useMemo } from 'react';
import { StyleSheet } from 'react-native';
import Toast, { BaseToast } from 'react-native-toast-message';
import type { ToastConfigParams } from 'react-native-toast-message';

import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Colors } from '@/constants/theme';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';

export function ThemedToast() {
  const colorScheme = useAppColorScheme();
  const insets = useSafeAreaInsets();
  const scheme = colorScheme === 'dark' ? 'dark' : 'light';
  const c = Colors[scheme];

  const config = useMemo(() => {
    const shadowColor = scheme === 'dark' ? 'rgba(0,0,0,0.45)' : 'rgba(0,0,0,0.12)';

    function ThemedToastCard(props: ToastConfigParams<object>) {
      return (
        <BaseToast
          {...props}
          style={[
            styles.card,
            {
              backgroundColor: c.background,
              shadowColor,
            },
          ]}
          text1Style={[styles.text1, { color: c.text }]}
          text2Style={[styles.text2, { color: c.icon }]}
        />
      );
    }

    return {
      info: ThemedToastCard,
      error: ThemedToastCard,
    };
  }, [c.background, c.text, c.icon, scheme]);

  const topOffset = Math.max(insets.top, 12) + 8;

  return (
    <Toast
      position="top"
      topOffset={topOffset}
      config={config}
      visibilityTime={4000}
      swipeable
    />
  );
}

const styles = StyleSheet.create({
  card: {
    borderLeftWidth: 0,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 4,
  },
  text1: {
    fontSize: 14,
    fontWeight: '600',
  },
  text2: {
    fontSize: 12,
  },
});
