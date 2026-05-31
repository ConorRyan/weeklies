import { useRouter, type Href } from 'expo-router';
import { type PropsWithChildren } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { Colors } from '@/constants/theme';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';

type TabActionButtonProps = {
  label: string;
  href?: Href;
  onPress?: () => void;
  tint?: string;
  borderColor?: string;
  textColor?: string;
  disabled?: boolean;
};

export function TabActionRow({
  children,
  embedded = false,
}: PropsWithChildren<{ embedded?: boolean }>) {
  return <View style={[styles.row, embedded && styles.rowEmbedded]}>{children}</View>;
}

export function TabActionButton({
  label,
  href,
  onPress,
  tint,
  borderColor,
  textColor,
  disabled = false,
}: TabActionButtonProps) {
  const router = useRouter();
  const colorScheme = useAppColorScheme();
  const defaultTint = tint ?? Colors[colorScheme ?? 'light'].tint;
  const border = borderColor ?? defaultTint;
  const text = textColor ?? defaultTint;

  const handlePress = () => {
    if (disabled) {
      return;
    }
    if (href) {
      router.push(href);
      return;
    }
    onPress?.();
  };

  return (
    <View style={styles.buttonSlot}>
      <Pressable
        disabled={disabled}
        onPress={handlePress}
        style={({ pressed }) => [
          styles.button,
          { borderColor: border },
          disabled && styles.disabled,
          pressed && !disabled && styles.pressed,
        ]}>
        <ThemedText
          type="defaultSemiBold"
          style={{ color: text, textAlign: 'center' }}
          numberOfLines={2}>
          {label}
        </ThemedText>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'stretch',
    gap: 8,
    marginHorizontal: 16,
    marginBottom: 8,
  },
  rowEmbedded: {
    marginHorizontal: 0,
    marginBottom: 0,
  },
  buttonSlot: {
    flex: 1,
    minWidth: 0,
  },
  button: {
    width: '100%',
    paddingVertical: 10,
    paddingHorizontal: 8,
    borderRadius: 10,
    borderWidth: 1,
    borderStyle: 'solid',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 44,
  },
  pressed: {
    opacity: 0.85,
  },
  disabled: {
    opacity: 0.45,
  },
});

