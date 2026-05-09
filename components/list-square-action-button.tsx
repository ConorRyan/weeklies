import { Pressable, StyleSheet, Text } from 'react-native';

import { useAppColorScheme } from '@/hooks/use-app-color-scheme';

export const LIST_SQUARE_ACTION_SIZE = 32;

/** Shorter rows where the default square would dominate row height (e.g. shopping list). */
export const LIST_SQUARE_ACTION_COMPACT_SIZE = 28;

type Accent = { readonly light: string; readonly dark: string };

type Props = {
  label: '+' | '-';
  accent: Accent;
  onPress: () => void;
  disabled?: boolean;
  /** Defaults to {@link LIST_SQUARE_ACTION_SIZE}. */
  size?: number;
};

export function ListSquareActionButton({
  label,
  accent,
  onPress,
  disabled,
  size = LIST_SQUARE_ACTION_SIZE,
}: Props) {
  const colorScheme = useAppColorScheme();
  const backgroundColor = colorScheme === 'dark' ? accent.dark : accent.light;
  const labelFontSize = Math.round(size * (18 / LIST_SQUARE_ACTION_SIZE));
  const touchPadding = size < LIST_SQUARE_ACTION_SIZE ? 8 : 0;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      hitSlop={
        touchPadding > 0
          ? { top: touchPadding, bottom: touchPadding, left: touchPadding, right: touchPadding }
          : undefined
      }
      onPress={onPress}
      style={({ pressed }) => [
        styles.pressable,
        {
          width: size,
          height: size,
          backgroundColor,
          opacity: disabled ? 0.4 : pressed ? 0.88 : 1,
        },
      ]}>
      <Text style={[styles.label, { fontSize: labelFontSize }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressable: {
    alignItems: 'center',
    borderRadius: 6,
    justifyContent: 'center',
  },
  label: {
    color: '#FFFFFF',
    fontWeight: '600',
    marginTop: -1,
  },
});
