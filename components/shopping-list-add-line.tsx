import { useRef, useState } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';

import { ShoppingListScreenHeader } from '@/constants/theme';
import { useShoppingList } from '@/store/shopping-list';
import {
  LIST_SQUARE_ACTION_COMPACT_SIZE,
  ListSquareActionButton,
} from './list-square-action-button';
import { ThemedTextInput } from './themed-text-input';

export function ShoppingListAddLine() {
  const { addItem } = useShoppingList();

  const [label, setLabel] = useState('');
  const inputRef = useRef<TextInput>(null);

  const submit = () => {
    const trimmed = label.trim();
    if (!trimmed) {
      return;
    }
    addItem({ label: trimmed });
    setLabel('');
    requestAnimationFrame(() => {
      inputRef.current?.focus();
    });
  };

  return (
    <View style={styles.row}>
      <ThemedTextInput
        ref={inputRef}
        style={styles.input}
        placeholder="Item"
        value={label}
        onChangeText={setLabel}
        blurOnSubmit={false}
        returnKeyType="done"
        onSubmitEditing={submit}
        includeFontPadding={false}
        textAlignVertical="center"
      />
      <ListSquareActionButton
        accent={ShoppingListScreenHeader}
        label="+"
        size={LIST_SQUARE_ACTION_COMPACT_SIZE}
        onPress={submit}
      />
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
  input: {
    flex: 1,
    fontSize: 15,
    lineHeight: 20,
    margin: 0,
    paddingVertical: 0,
    paddingHorizontal: 0,
  },
});
