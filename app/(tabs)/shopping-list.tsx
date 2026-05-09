import { StyleSheet } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ShoppingListAddLine } from '@/components/shopping-list-add-line';
import { ShoppingListItem } from '@/components/shopping-list-item';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Fonts, ShoppingListScreenHeader } from '@/constants/theme';
import { useShoppingList } from '@/store/shopping-list';




export default function ShoppingListScreen() {
  const { items, toggleItem } = useShoppingList();

  return (
    <ParallaxScrollView
      headerBackgroundColor={ShoppingListScreenHeader}
      headerImage={
        <IconSymbol
          size={310}
          color="#FFFFFF"
          name="cart"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText
          type="title"
          style={{
            fontFamily: Fonts.rounded,
          }}>
          Shopping List
        </ThemedText>
      </ThemedView>
      {items.map((item) => (
        <ShoppingListItem
          key={item.id}
          {...item}
          onToggle={() => toggleItem(item.id)
          }
        />
      ))}
      <ShoppingListAddLine></ShoppingListAddLine>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    bottom: -90,
    left: -35,
    position: 'absolute',
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
  },
});
