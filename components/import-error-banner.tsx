import { StyleSheet } from 'react-native';

import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import type { RecipeImportErrorDisplay } from '@/utils/recipe-import/import-errors';

type ImportErrorBannerProps = {
  error: RecipeImportErrorDisplay;
};

export function ImportErrorBanner({ error }: ImportErrorBannerProps) {
  return (
    <ThemedView style={styles.errorBox} accessibilityRole="alert">
      <ThemedText type="defaultSemiBold" style={styles.errorTitle}>
        {error.title}
      </ThemedText>
      <ThemedText style={styles.errorMessage}>{error.message}</ThemedText>
      {error.hint ? <ThemedText style={styles.errorHint}>{error.hint}</ThemedText> : null}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  errorBox: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#d11a2a',
    backgroundColor: 'rgba(209, 26, 42, 0.08)',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  errorTitle: {
    color: '#d11a2a',
  },
  errorMessage: {
    color: '#d11a2a',
    lineHeight: 20,
  },
  errorHint: {
    color: '#d11a2a',
    fontSize: 13,
    lineHeight: 18,
    opacity: 0.9,
  },
});
