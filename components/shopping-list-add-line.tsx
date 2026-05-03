import { Button, StyleSheet, TouchableOpacity } from 'react-native';

import { useShoppingList } from '@/store/shopping-list';
import { useState } from 'react';
import { ThemedTextInput } from './themed-text-input';


export function ShoppingListAddLine() {
  const { addItem } = useShoppingList();

  const [name, setName] = useState('');
  const [quantity, setQuantity] = useState('');

  const onPress = () => {
    addItem({ name: name, quantity: quantity })
    setName("");
    setQuantity("");
  };

  return (
    <TouchableOpacity style={styles.row} >
      <ThemedTextInput style={[styles.name]} placeholder='Name' value={name} onChangeText={setName}></ThemedTextInput>
      <ThemedTextInput style={styles.quantity} placeholder='Qty' value={quantity} onChangeText={setQuantity}></ThemedTextInput>
      <Button title="+" onPress={onPress}></Button>
    </TouchableOpacity>
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
  name: {
    flex: 1,
    fontSize: 16,
  },
  quantity: {
    fontSize: 14,
    opacity: 0.6,
  },
});