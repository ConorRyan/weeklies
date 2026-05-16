import { ThemedText } from '@/components/themed-text';
import { Pressable, StyleSheet, View } from 'react-native';

type Props = {
  id: string;
  label: string;
  checked: boolean;
  onToggle: () => void;
};

export function ShoppingListItem({ label, checked, onToggle }: Props) {
  return (
    <View style={styles.row}>
      <Pressable
        hitSlop={{ top: 22, bottom: 22, left: 18, right: 12 }}
        pressRetentionOffset={{ top: 24, bottom: 24, left: 24, right: 24 }}
        onPress={onToggle}
        style={({ pressed }) => [styles.lineToggle, pressed && styles.lineTogglePressed]}>
        <View style={[styles.checkbox, checked && styles.checked]} />
        <ThemedText style={[styles.label, checked && styles.strikethrough]}>{label}</ThemedText>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 3,
    paddingHorizontal: 10,
    gap: 6,
  },
  lineToggle: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    minWidth: 0,
  },
  lineTogglePressed: {
    opacity: 0.85,
  },
  checkbox: {
    width: 18,
    height: 18,
    borderRadius: 3,
    borderWidth: 2,
    borderColor: '#888',
  },
  checked: {
    backgroundColor: '#4CAF50',
    borderColor: '#4CAF50',
  },
  label: {
    flex: 1,
    fontSize: 15,
    lineHeight: 20,
  },
  strikethrough: {
    textDecorationLine: 'line-through',
    opacity: 0.5,
  },
});
