import { forwardRef } from 'react';
import { useThemeColor } from '@/hooks/use-theme-color';
import { TextInput, TextInputProps } from 'react-native';

type Props = TextInputProps & {
  lightColor?: string;
  darkColor?: string;
};

export const ThemedTextInput = forwardRef<TextInput, Props>(function ThemedTextInput(
  { lightColor, darkColor, style, ...rest },
  ref
) {
  const color = useThemeColor({ light: lightColor, dark: darkColor }, 'text');

  return <TextInput ref={ref} style={[{ color }, style]} placeholderTextColor="#888" {...rest} />;
});