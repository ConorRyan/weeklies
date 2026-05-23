import { Link, Stack, router } from 'expo-router';
import { useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from 'react-native';

import { RecipeAddForm } from '@/components/recipe-add-form';
import { ThemedText } from '@/components/themed-text';
import { ThemedTextInput } from '@/components/themed-text-input';
import { ThemedView } from '@/components/themed-view';
import { hasMistralApiKey, useImportSecrets } from '@/contexts/import-secrets';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import type { NewRecipe } from '@/store/recipes';
import { useRecipes } from '@/store/recipes';
import {
  importRecipeFromUrlWithMistral,
  importRecipeWithMistral,
  isValidRecipeUrl,
} from '@/utils/recipe-import';

const WEB_HTML_HINT =
  'URL import is not available in the browser build. Open the recipe page in your browser, copy the page HTML (View Source or developer tools), and paste it below.';

export default function RecipeImportScreen() {
  const colorScheme = useAppColorScheme();
  const inputBorderColor = colorScheme === 'dark' ? '#FFFFFF' : '#9A9A9A';
  const { secrets } = useImportSecrets();
  const addRecipe = useRecipes((s) => s.addRecipe);

  const isWeb = Platform.OS === 'web';
  const [url, setUrl] = useState('');
  const [htmlPaste, setHtmlPaste] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState<NewRecipe | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const keyReady = hasMistralApiKey(secrets);

  const cancelImport = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setBusy(false);
  };

  const runImport = () => {
    if (!keyReady) {
      setError('Add your Mistral API key in Settings → Recipe import.');
      return;
    }

    setError('');
    setPreview(null);

    if (isWeb) {
      if (!htmlPaste.trim()) {
        setError('Paste the recipe page HTML.');
        return;
      }
    } else {
      if (!url.trim()) {
        setError('Enter a recipe URL.');
        return;
      }
      if (!isValidRecipeUrl(url)) {
        setError('Enter a valid http or https URL.');
        return;
      }
    }

    const controller = new AbortController();
    abortRef.current = controller;
    setBusy(true);

    const task = isWeb
      ? importRecipeWithMistral({
          html: htmlPaste,
          apiKey: secrets.mistralApiKey,
          signal: controller.signal,
        })
      : importRecipeFromUrlWithMistral({
          url,
          apiKey: secrets.mistralApiKey,
          signal: controller.signal,
        });

    void task
      .then((recipe) => {
        setPreview(recipe);
      })
      .catch((e: unknown) => {
        setError(e instanceof Error ? e.message : 'Import failed.');
      })
      .finally(() => {
        if (abortRef.current === controller) {
          abortRef.current = null;
        }
        setBusy(false);
      });
  };

  if (preview) {
    return (
      <ThemedView style={styles.container}>
        <Stack.Screen options={{ title: 'Review import' }} />
        <ScrollView keyboardShouldPersistTaps="handled">
          <ThemedText style={styles.previewHint}>
            Review the imported recipe, edit if needed, then save. Ingredient amounts were scaled
            to one portion (the recipe&apos;s serving count was used only for scaling).
          </ThemedText>
          <RecipeAddForm
            initialRecipe={preview}
            onAddRecipe={(recipe) => {
              addRecipe(recipe);
              router.back();
            }}
          />
          <Pressable
            onPress={() => setPreview(null)}
            style={styles.secondaryAction}
            accessibilityRole="button">
            <ThemedText type="link">Import again</ThemedText>
          </Pressable>
        </ScrollView>
      </ThemedView>
    );
  }

  return (
    <ThemedView style={styles.container}>
      <Stack.Screen options={{ title: 'Import with AI' }} />
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled">
        {!keyReady ? (
          <ThemedView style={styles.keyWarning}>
            <ThemedText style={styles.errorText}>
              Add your Mistral API key in Settings before importing.
            </ThemedText>
            <Link href="/(tabs)/settings" asChild>
              <Pressable accessibilityRole="button">
                <ThemedText type="link">Open Settings</ThemedText>
              </Pressable>
            </Link>
          </ThemedView>
        ) : null}

        {isWeb ? (
          <>
            <ThemedText style={styles.hint}>{WEB_HTML_HINT}</ThemedText>
            <ThemedText style={styles.fieldHeading}>Page HTML</ThemedText>
            <ThemedTextInput
              value={htmlPaste}
              onChangeText={setHtmlPaste}
              placeholder="Paste HTML here"
              multiline
              style={[styles.htmlInput, { borderColor: inputBorderColor }]}
              editable={!busy}
              accessibilityLabel="Recipe page HTML"
            />
          </>
        ) : (
          <>
            <ThemedText style={styles.fieldHeading}>Recipe URL</ThemedText>
            <ThemedTextInput
              value={url}
              onChangeText={setUrl}
              placeholder="https://…"
              autoCapitalize="none"
              autoCorrect={false}
              keyboardType="url"
              style={[styles.urlInput, { borderColor: inputBorderColor }]}
              editable={!busy}
              accessibilityLabel="Recipe URL"
            />
          </>
        )}

        <View style={styles.actions}>
          <Pressable
            onPress={runImport}
            disabled={busy || !keyReady}
            style={[styles.primaryButton, (busy || !keyReady) && styles.buttonDisabled]}
            accessibilityRole="button"
            accessibilityLabel="Import recipe with AI">
            {busy ? (
              <ActivityIndicator color={colorScheme === 'dark' ? '#fff' : '#000'} />
            ) : (
              <ThemedText type="defaultSemiBold">Import</ThemedText>
            )}
          </Pressable>
          {busy ? (
            <Pressable onPress={cancelImport} accessibilityRole="button">
              <ThemedText type="link">Cancel</ThemedText>
            </Pressable>
          ) : null}
        </View>

        {error ? <ThemedText style={styles.errorText}>{error}</ThemedText> : null}

        <ThemedText style={styles.footerNote}>
          Import sends page content to Mistral using your API key. Amounts are stored for one
          portion; the shopping list scales them when you add a planned recipe. Requires an
          internet connection.
        </ThemedText>
      </ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 12,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingBottom: 32,
    gap: 10,
  },
  keyWarning: {
    gap: 8,
    marginBottom: 8,
  },
  hint: {
    fontSize: 14,
    lineHeight: 20,
    opacity: 0.85,
  },
  fieldHeading: {
    fontSize: 14,
    fontWeight: '600',
    marginTop: 4,
  },
  urlInput: {
    borderRadius: 6,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  htmlInput: {
    borderRadius: 6,
    borderWidth: 1,
    minHeight: 160,
    paddingHorizontal: 10,
    paddingVertical: 8,
    textAlignVertical: 'top',
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    marginTop: 8,
  },
  primaryButton: {
    borderRadius: 6,
    borderWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 10,
    minWidth: 100,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  errorText: {
    color: '#d11a2a',
  },
  footerNote: {
    fontSize: 13,
    opacity: 0.7,
    marginTop: 12,
    lineHeight: 18,
  },
  previewHint: {
    paddingHorizontal: 16,
    marginBottom: 4,
    fontSize: 14,
    opacity: 0.85,
  },
  secondaryAction: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
});
