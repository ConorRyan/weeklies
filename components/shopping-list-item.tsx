import { ListSquareActionButton } from '@/components/list-square-action-button';
import { ThemedText } from '@/components/themed-text';
import { ShoppingListScreenHeader } from '@/constants/theme';
import { useShoppingList } from '@/store/shopping-list';
import { StyleSheet, TouchableOpacity, View } from 'react-native';

type Props = {
  id: string;
  label: string;
  checked: boolean;
  onToggle: () => void;
};

export function ShoppingListItem({ id, label, checked, onToggle }: Props) {
  const { removeItem } = useShoppingList();

  const onPress = (): void => {
    removeItem(id);
  };

  return (
    <View style={styles.row}>
      <TouchableOpacity onPress={onToggle}>
        <View style={[styles.checkbox, checked && styles.checked]} />
      </TouchableOpacity>
      <ThemedText style={[styles.label, checked && styles.strikethrough]}>{label}</ThemedText>
      <ListSquareActionButton accent={ShoppingListScreenHeader} label="-" onPress={onPress} />
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    gap: 12,
  },
  checkbox: {
    width: 22,
    height: 22,
    borderRadius: 4,
    borderWidth: 2,
    borderColor: '#888',
  },
  checked: {
    backgroundColor: '#4CAF50',
    borderColor: '#4CAF50',
  },
  label: {
    flex: 1,
    fontSize: 16,
  },
  strikethrough: {
    textDecorationLine: 'line-through',
    opacity: 0.5,
  },
});
