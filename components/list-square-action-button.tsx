import { Pressable, StyleSheet, Text } from 'react-native';

import { useColorScheme } from '@/hooks/use-color-scheme';

export const LIST_SQUARE_ACTION_SIZE = 32;

type Accent = { readonly light: string; readonly dark: string };

type Props = {
  label: '+' | '-';
  accent: Accent;
  onPress: () => void;
  disabled?: boolean;
};

export function ListSquareActionButton({ label, accent, onPress, disabled }: Props) {
  const colorScheme = useColorScheme();
  const backgroundColor = colorScheme === 'dark' ? accent.dark : accent.light;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.pressable,
        {
          backgroundColor,
          opacity: disabled ? 0.4 : pressed ? 0.88 : 1,
        },
      ]}>
      <Text style={styles.label}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressable: {
    alignItems: 'center',
    borderRadius: 6,
    height: LIST_SQUARE_ACTION_SIZE,
    justifyContent: 'center',
    width: LIST_SQUARE_ACTION_SIZE,
  },
  label: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
    marginTop: -1,
  },
});
