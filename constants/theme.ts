/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import { Platform } from 'react-native';

const tintColorLight = '#0a7ea4';
const tintColorDark = '#fff';

/** Parallax header backgrounds per tab — reuse for list action buttons on that screen. */
export const RecipesScreenHeader = {
  light: '#FF4D4D',
  dark: '#C72727',
} as const;

export const ShoppingListScreenHeader = {
  light: '#00DD77',
  dark: '#00A45E',
} as const;

export const SettingsScreenHeader = {
  light: '#9333EA',
  dark: '#7028C0',
} as const;

export const Colors = {
  light: {
    text: '#11181C',
    background: '#fff',
    tint: tintColorLight,
    icon: '#687076',
  },
  dark: {
    text: '#ECEDEE',
    background: '#151718',
    tint: tintColorDark,
    icon: '#9BA1A6',
  },
};

/** Rounded UI font for screen titles (see Apple Human Interface Guidelines). */
export const Fonts = Platform.select({
  ios: { rounded: 'ui-rounded' },
  default: { rounded: 'normal' },
  web: {
    rounded:
      "'SF Pro Rounded', 'Hiragino Maru Gothic ProN', Meiryo, 'MS PGothic', sans-serif",
  },
}) ?? { rounded: 'normal' };
