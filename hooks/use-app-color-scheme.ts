import { useThemePreference } from '@/contexts/settings';

/** Resolved light/dark appearance from app Settings (not necessarily the OS theme). */
export function useAppColorScheme() {
  return useThemePreference().colorScheme;
}
