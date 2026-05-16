import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolViewProps, SymbolWeight } from 'expo-symbols';
import { ComponentProps, useEffect, useState } from 'react';
import {
  OpaqueColorValue,
  Platform,
  StyleSheet,
  View,
  type StyleProp,
  type TextStyle,
  type ViewStyle,
} from 'react-native';

type SFSymbolName = Extract<SymbolViewProps['name'], string>;
type MaterialIconName = ComponentProps<typeof MaterialIcons>['name'];
type IconSymbolName = keyof typeof MAPPING;

const MAPPING = {
  calendar: 'calendar-month',
  book: 'book',
  cart: 'shopping-cart',
  'chevron.right': 'chevron-right',
  gearshape: 'settings',
  'info.circle': 'info-outline',
} as const satisfies Partial<Record<SFSymbolName, MaterialIconName>>;

/**
 * Material Icons on Android and web (iOS uses icon-symbol.ios.tsx).
 * On web, icon font glyphs are deferred until after mount so static HTML matches the first client paint.
 */
export function IconSymbol({
  name,
  size = 24,
  color,
  style,
}: {
  name: IconSymbolName;
  size?: number;
  color: string | OpaqueColorValue;
  style?: StyleProp<TextStyle>;
  weight?: SymbolWeight;
}) {
  const [iconReady, setIconReady] = useState(Platform.OS !== 'web');

  useEffect(() => {
    if (Platform.OS === 'web') {
      setIconReady(true);
    }
  }, []);

  if (!iconReady) {
    const flat = StyleSheet.flatten(style) as ViewStyle | undefined;
    return <View style={[{ width: size, height: size }, flat]} />;
  }

  return <MaterialIcons color={color} size={size} name={MAPPING[name]} style={style} />;
}
