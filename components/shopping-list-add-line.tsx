import { StyleSheet, View } from 'react-native';

import { ShoppingListScreenHeader } from '@/constants/theme';
import { useShoppingList } from '@/store/shopping-list';
import { useState } from 'react';
import { ListSquareActionButton } from './list-square-action-button';
import { ThemedTextInput } from './themed-text-input';

export function ShoppingListAddLine() {
  const { addItem } = useShoppingList();

  const [label, setLabel] = useState('');

  const onPress = () => {
    const trimmed = label.trim();
    if (!trimmed) {
      return;
    }
    addItem({ label: trimmed });
    setLabel('');
  };

  return (
    <View style={styles.row}>
      <ThemedTextInput
        style={styles.input}
        placeholder="Item"
        value={label}
        onChangeText={setLabel}
      />
      <ListSquareActionButton accent={ShoppingListScreenHeader} label="+" onPress={onPress} />
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
  input: {
    flex: 1,
    fontSize: 16,
  },
});
