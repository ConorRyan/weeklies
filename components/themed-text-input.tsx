import { useThemeColor } from '@/hooks/use-theme-color';
import { TextInput, TextInputProps } from 'react-native';

type Props = TextInputProps & {
  lightColor?: string;
  darkColor?: string;
};

export function ThemedTextInput({ lightColor, darkColor, style, ...rest }: Props) {
  const color = useThemeColor({ light: lightColor, dark: darkColor }, 'text');

  return (
    <TextInput
      style={[{ color }, style]}
      placeholderTextColor='#888'
      {...rest}
    />
  );
}