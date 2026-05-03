import { ThemedText } from '@/components/themed-text';
import { Button, StyleSheet, TouchableOpacity, View } from 'react-native';

import { useShoppingList } from '@/store/shopping-list';

type Props = {
    id: string;
    name: string;
    quantity: string;
    checked: boolean;
    onToggle: () => void;
};

export function ShoppingListItem({ id, name, quantity, checked, onToggle }: Props) {
    const { removeItem } = useShoppingList();

    const onPress = (): void => {
        removeItem(id);
    };

    return (
        <View style={styles.row}>
            <TouchableOpacity onPress={onToggle}>
                <View style={[styles.checkbox, checked && styles.checked]} />
            </TouchableOpacity>
            <ThemedText style={[styles.name, checked && styles.strikethrough]}>
                {name}
            </ThemedText>
            <ThemedText style={styles.quantity}>{quantity}</ThemedText>
            <Button title="-" onPress={onPress} />
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
    name: {
        flex: 1,
        fontSize: 16,
    },
    strikethrough: {
        textDecorationLine: 'line-through',
        opacity: 0.5,
    },
    quantity: {
        fontSize: 14,
        opacity: 0.6,
    },
});