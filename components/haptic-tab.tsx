// expo-router vendors its own react-navigation types; use its
// BottomTabBarButtonProps and PlatformPressable so the component matches
// the tabBarButton option.
import type { BottomTabBarButtonProps } from 'expo-router/build/react-navigation/bottom-tabs';
import { PlatformPressable } from 'expo-router/build/react-navigation/elements';
import * as Haptics from 'expo-haptics';
import type { ReactNode } from 'react';

export function HapticTab(props: BottomTabBarButtonProps): ReactNode {
  return (
    <PlatformPressable
      {...props}
      onPressIn={(ev) => {
        if (process.env.EXPO_OS === 'ios') {
          // Add a soft haptic feedback when pressing down on the tabs.
          Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
        }
        props.onPressIn?.(ev);
      }}
    />
  );
}
