import { Link, Stack, router } from 'expo-router';
import { useRef, useState } from 'react';
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
} from 'react-native';

import { ImportErrorBanner } from '@/components/import-error-banner';
import { RecipeAddForm } from '@/components/recipe-add-form';
import { TabActionButton, TabActionRow } from '@/components/tab-action-button';
import { ThemedText } from '@/components/themed-text';
import { ThemedTextInput } from '@/components/themed-text-input';
import { ThemedView } from '@/components/themed-view';
import { hasMistralApiKey, useImportSecrets } from '@/contexts/import-secrets';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import type { NewRecipe } from '@/store/recipes';
import { useRecipes } from '@/store/recipes';
import {
  displayRecipeImportError,
  formatRecipeImportError,
  importRecipeFromHtmlWithScraper,
  importRecipeFromUrlWithScraper,
  importRecipeFromUrlWithMistral,
  importRecipeWithMistral,
  isValidRecipeUrl,
  recipeImportErrors,
  type RecipeImportErrorDisplay,
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
  const [error, setError] = useState<RecipeImportErrorDisplay | null>(null);
  const [preview, setPreview] = useState<NewRecipe | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const keyReady = hasMistralApiKey(secrets);

  const cancelImport = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setBusy(false);
  };

  const validateInput = (): boolean => {
    if (isWeb) {
      if (!htmlPaste.trim()) {
        setError(displayRecipeImportError(recipeImportErrors.emptyContent()));
        return false;
      }
    } else {
      if (!url.trim()) {
        setError(displayRecipeImportError(recipeImportErrors.missingUrl()));
        return false;
      }
      if (!isValidRecipeUrl(url)) {
        setError(displayRecipeImportError(recipeImportErrors.invalidUrl()));
        return false;
      }
    }
    return true;
  };

  const runImportTask = (getTask: (signal: AbortSignal) => Promise<NewRecipe>) => {
    const controller = new AbortController();
    abortRef.current = controller;
    setBusy(true);

    void getTask(controller.signal)
      .then((recipe) => {
        setPreview(recipe);
      })
      .catch((e: unknown) => {
        const formatted = formatRecipeImportError(e);
        if (formatted) {
          setError(formatted);
        }
      })
      .finally(() => {
        if (abortRef.current === controller) {
          abortRef.current = null;
        }
        setBusy(false);
      });
  };

  const runScraperImport = () => {
    setError(null);
    setPreview(null);
    if (!validateInput()) {
      return;
    }

    runImportTask((signal) =>
      isWeb
        ? importRecipeFromHtmlWithScraper({
            html: htmlPaste,
            signal,
          })
        : importRecipeFromUrlWithScraper({
            url,
            signal,
          })
    );
  };

  const runAiImport = () => {
    if (!keyReady) {
      setError(displayRecipeImportError(recipeImportErrors.mistralKeyMissing()));
      return;
    }

    setError(null);
    setPreview(null);
    if (!validateInput()) {
      return;
    }

    runImportTask((signal) =>
      isWeb
        ? importRecipeWithMistral({
            html: htmlPaste,
            apiKey: secrets.mistralApiKey,
            signal,
          })
        : importRecipeFromUrlWithMistral({
            url,
            apiKey: secrets.mistralApiKey,
            signal,
          })
    );
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
      <Stack.Screen options={{ title: 'Import recipe' }} />
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled">
        {isWeb ? (
          <>
            <ThemedText style={styles.hint}>{WEB_HTML_HINT}</ThemedText>
            <ThemedText style={styles.fieldHeading}>Page HTML</ThemedText>
            <ThemedTextInput
              value={htmlPaste}
              onChangeText={(text) => {
                setHtmlPaste(text);
                setError(null);
              }}
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
              onChangeText={(text) => {
                setUrl(text);
                setError(null);
              }}
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

        <TabActionRow embedded>
          <TabActionButton
            label="Import"
            onPress={runScraperImport}
            disabled={busy}
          />
          <TabActionButton
            label="Import with AI"
            onPress={runAiImport}
            disabled={busy || !keyReady}
          />
        </TabActionRow>

        {busy ? (
          <ThemedView style={styles.busyRow}>
            <ActivityIndicator color={colorScheme === 'dark' ? '#fff' : '#000'} />
            <Pressable onPress={cancelImport} accessibilityRole="button">
              <ThemedText type="link">Cancel</ThemedText>
            </Pressable>
          </ThemedView>
        ) : null}

        {!keyReady ? (
          <ThemedView style={styles.keyHint}>
            <ThemedText style={styles.hint}>
              Import with AI needs a Mistral API key in Settings.
            </ThemedText>
            <Link href="/(tabs)/settings" asChild>
              <Pressable accessibilityRole="button">
                <ThemedText type="link">Open Settings</ThemedText>
              </Pressable>
            </Link>
          </ThemedView>
        ) : null}

        {error ? <ImportErrorBanner error={error} /> : null}

        <ThemedText style={styles.footerNote}>
          Import reads recipe data from JSON-LD on the page on this device — no API key
          needed. Import with AI sends page content to Mistral using your key. Amounts are stored
          for one portion; the shopping list scales them when you add a planned recipe. Requires an
          internet connection to fetch the page (native) or when using AI.
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
  keyHint: {
    gap: 8,
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
  busyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
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
