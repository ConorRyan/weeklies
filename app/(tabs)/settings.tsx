import { Link } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Dimensions, Modal, Pressable, StyleSheet, Switch, Text, View } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedTextInput } from '@/components/themed-text-input';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors, Fonts, SettingsScreenHeader } from '@/constants/theme';
import { useImportSecrets } from '@/contexts/import-secrets';
import {
  isValidPortionsPerDay,
  usePortionsPerDay,
  useSettings,
  useThemePreference,
} from '@/contexts/settings';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { exportBackupToFilesystem, pickAndImportBackup } from '@/utils/backup';
import {
  formatBackupError,
  isUserCanceledBackup,
} from '@/utils/backup-errors';
import { notifyError, notifyInfo } from '@/utils/notify';

const PORTIONS_INFO_TEXT = 'Used to scale recipe amounts on your shopping list. Will only work if numeric quantities are included in ingredients.';

const MISTRAL_KEY_INFO_TEXT =
  'Your Mistral API key is stored only on this device and is not included in backup export or import. Get a key at console.mistral.ai.';

export default function SettingsScreen() {
  const colorScheme = useAppColorScheme();
  const { setColorScheme } = useThemePreference();
  const { portionsPerDay, setPortionsPerDay } = usePortionsPerDay();
  const { settings, replaceSettings } = useSettings();
  const { secrets, setMistralApiKey, clearMistralApiKey } = useImportSecrets();
  const [backupBusy, setBackupBusy] = useState(false);
  const [mistralKeyText, setMistralKeyText] = useState(() => secrets.mistralApiKey);
  const [lastMistralKey, setLastMistralKey] = useState(secrets.mistralApiKey);
  const [mistralKeyInfoOpen, setMistralKeyInfoOpen] = useState(false);
  const [mistralKeyInfoAnchor, setMistralKeyInfoAnchor] = useState<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);
  const mistralKeyInfoRef = useRef<View>(null);
  const [portionsText, setPortionsText] = useState(() => String(portionsPerDay));
  const [lastPortionsPerDay, setLastPortionsPerDay] = useState(portionsPerDay);
  const [portionsInfoOpen, setPortionsInfoOpen] = useState(false);
  const [portionsInfoAnchor, setPortionsInfoAnchor] = useState<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);
  const portionsInfoRef = useRef<View>(null);
  const mountedRef = useRef(true);
  const tint = Colors[colorScheme].tint;
  const schemeKey = colorScheme === 'dark' ? 'dark' : 'light';

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // Adjust the input text when the external value changes; render-time sync
  // instead of an effect.
  if (portionsPerDay !== lastPortionsPerDay) {
    setLastPortionsPerDay(portionsPerDay);
    setPortionsText(String(portionsPerDay));
  }
  if (secrets.mistralApiKey !== lastMistralKey) {
    setLastMistralKey(secrets.mistralApiKey);
    setMistralKeyText(secrets.mistralApiKey);
  }

  const openMistralKeyInfo = () => {
    mistralKeyInfoRef.current?.measureInWindow((x, y, width, height) => {
      setMistralKeyInfoAnchor({ x, y, width, height });
      setMistralKeyInfoOpen(true);
    });
  };

  const commitMistralKeyText = () => {
    setMistralApiKey(mistralKeyText.trim());
  };

  const openPortionsInfo = () => {
    portionsInfoRef.current?.measureInWindow((x, y, width, height) => {
      setPortionsInfoAnchor({ x, y, width, height });
      setPortionsInfoOpen(true);
    });
  };

  const commitPortionsText = () => {
    const n = Number(portionsText.trim());
    if (isValidPortionsPerDay(n)) {
      setPortionsPerDay(n);
      setPortionsText(String(n));
    } else {
      setPortionsText(String(portionsPerDay));
    }
  };

  const handleExport = () => {
    setBackupBusy(true);
    void exportBackupToFilesystem(settings)
      .catch((e: unknown) => {
        if (isUserCanceledBackup(e)) {
          notifyInfo('Export canceled');
          return;
        }
        notifyError('Export failed', formatBackupError(e, 'export'));
      })
      .finally(() => {
        if (mountedRef.current) {
          setBackupBusy(false);
        }
      });
  };

  const handleImport = () => {
    setBackupBusy(true);
    void pickAndImportBackup(replaceSettings)
      .catch((e: unknown) => {
        if (isUserCanceledBackup(e)) {
          notifyInfo('Import canceled');
          return;
        }
        notifyError('Import failed', formatBackupError(e, 'import'));
      })
      .finally(() => {
        if (mountedRef.current) {
          setBackupBusy(false);
        }
      });
  };

  return (
    <ParallaxScrollView
      headerBackgroundColor={SettingsScreenHeader}
      headerImage={
        <IconSymbol
          size={310}
          color="#FFFFFF"
          name="gearshape"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText
          type="title"
          style={{
            fontFamily: Fonts.rounded,
          }}>
          Settings
        </ThemedText>
      </ThemedView>

      <ThemedText style={[styles.sectionHeading, styles.sectionHeadingFirst]} accessibilityRole="header">
        Appearance
      </ThemedText>
      <ThemedView style={styles.row}>
        <ThemedText style={styles.label} accessibilityRole="text">
          Dark mode
        </ThemedText>
        <Switch
          value={colorScheme === 'dark'}
          onValueChange={(on) => setColorScheme(on ? 'dark' : 'light')}
          trackColor={{ false: '#767577', true: '#81b0ff' }}
          thumbColor={colorScheme === 'dark' ? tint : '#f4f3f4'}
          accessibilityLabel="Dark mode"
          accessibilityRole="switch"
        />
      </ThemedView>

      <ThemedText style={[styles.sectionHeading, styles.sectionHeadingSpacing]} accessibilityRole="header">
        Planning
      </ThemedText>
      <ThemedView style={styles.portionsRow}>
        <ThemedView style={styles.portionsLabelRow}>
          <ThemedText style={styles.label}>Portions per day</ThemedText>
          <View ref={portionsInfoRef} collapsable={false} style={styles.infoIconWrap}>
            <Pressable
              onPress={openPortionsInfo}
              hitSlop={8}
              accessibilityLabel="About portions per day"
              accessibilityRole="button">
              <IconSymbol name="info.circle" size={20} color={Colors[colorScheme].icon} />
            </Pressable>
          </View>
        </ThemedView>
        <ThemedTextInput
          keyboardType="decimal-pad"
          value={portionsText}
          onChangeText={setPortionsText}
          onBlur={commitPortionsText}
          style={styles.portionsInput}
          selectTextOnFocus
          accessibilityLabel="Portions per day"
        />
      </ThemedView>

      <Modal
        visible={portionsInfoOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setPortionsInfoOpen(false)}>
        <Pressable
          style={styles.infoBackdrop}
          onPress={() => setPortionsInfoOpen(false)}
          accessibilityLabel="Dismiss"
        />
        {portionsInfoAnchor ? (
          <View
            style={[
              styles.infoBubble,
              {
                backgroundColor: Colors[colorScheme].background,
                top: portionsInfoAnchor.y + portionsInfoAnchor.height + 8,
                left: (() => {
                  const w = Dimensions.get('window').width;
                  const bubbleW = Math.min(280, w - 24);
                  const idealLeft = portionsInfoAnchor.x + portionsInfoAnchor.width / 2 - bubbleW / 2;
                  return Math.max(12, Math.min(idealLeft, w - bubbleW - 12));
                })(),
                maxWidth: Math.min(280, Dimensions.get('window').width - 24),
              },
            ]}
            pointerEvents="box-none">
            <ThemedText style={styles.infoBubbleText}>{PORTIONS_INFO_TEXT}</ThemedText>
          </View>
        ) : null}
      </Modal>

      <ThemedText style={[styles.sectionHeading, styles.sectionHeadingSpacing]} accessibilityRole="header">
        Recipe import
      </ThemedText>
      <ThemedView style={styles.mistralKeyBlock}>
        <ThemedView style={styles.portionsLabelRow}>
          <ThemedText style={styles.label}>Mistral API key</ThemedText>
          <View ref={mistralKeyInfoRef} collapsable={false} style={styles.infoIconWrap}>
            <Pressable
              onPress={openMistralKeyInfo}
              hitSlop={8}
              accessibilityLabel="About Mistral API key"
              accessibilityRole="button">
              <IconSymbol name="info.circle" size={20} color={Colors[colorScheme].icon} />
            </Pressable>
          </View>
        </ThemedView>
        <ThemedTextInput
          value={mistralKeyText}
          onChangeText={setMistralKeyText}
          onBlur={commitMistralKeyText}
          placeholder="Paste API key"
          secureTextEntry
          autoCapitalize="none"
          autoCorrect={false}
          style={styles.mistralKeyInput}
          accessibilityLabel="Mistral API key"
        />
        {secrets.mistralApiKey.trim().length > 0 ? (
          <Pressable
            onPress={() => {
              clearMistralApiKey();
              setMistralKeyText('');
            }}
            accessibilityRole="button"
            accessibilityLabel="Clear Mistral API key"
            style={({ pressed }) => [styles.clearKeyRow, pressed && styles.dataRowPressed]}>
            <ThemedText type="link">Clear key</ThemedText>
          </Pressable>
        ) : null}
      </ThemedView>

      <Modal
        visible={mistralKeyInfoOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setMistralKeyInfoOpen(false)}>
        <Pressable
          style={styles.infoBackdrop}
          onPress={() => setMistralKeyInfoOpen(false)}
          accessibilityLabel="Dismiss"
        />
        {mistralKeyInfoAnchor ? (
          <View
            style={[
              styles.infoBubble,
              {
                backgroundColor: Colors[colorScheme].background,
                top: mistralKeyInfoAnchor.y + mistralKeyInfoAnchor.height + 8,
                left: (() => {
                  const w = Dimensions.get('window').width;
                  const bubbleW = Math.min(280, w - 24);
                  const idealLeft =
                    mistralKeyInfoAnchor.x + mistralKeyInfoAnchor.width / 2 - bubbleW / 2;
                  return Math.max(12, Math.min(idealLeft, w - bubbleW - 12));
                })(),
                maxWidth: Math.min(280, Dimensions.get('window').width - 24),
              },
            ]}
            pointerEvents="box-none">
            <ThemedText style={styles.infoBubbleText}>{MISTRAL_KEY_INFO_TEXT}</ThemedText>
          </View>
        ) : null}
      </Modal>

      <ThemedText style={[styles.sectionHeading, styles.sectionHeadingSpacing]} accessibilityRole="header">
        Data
      </ThemedText>
      <ThemedView style={styles.dataActions}>
        <Pressable
          onPress={handleExport}
          disabled={backupBusy}
          accessibilityRole="button"
          accessibilityLabel="Export data as JSON"
          style={({ pressed }) => [styles.dataRow, pressed && styles.dataRowPressed]}>
          <ThemedText type="link" style={backupBusy ? styles.dataLinkDisabled : undefined}>
            Export…
          </ThemedText>
        </Pressable>
        <Pressable
          onPress={handleImport}
          disabled={backupBusy}
          accessibilityRole="button"
          accessibilityLabel="Import backup from file. Warning: replaces all data."
          accessibilityHint="Import replaces recipes, weekly plan, shopping list, and settings."
          style={({ pressed }) => [styles.dataRow, pressed && styles.dataRowPressed]}>
          <ThemedText type="link" style={backupBusy ? styles.dataLinkDisabled : undefined}>
            Import…{' '}
            <Text style={[styles.importQualifier, { color: Colors[schemeKey].icon }]}>
              (WARNING: replaces all data)
            </Text>
          </ThemedText>
        </Pressable>
      </ThemedView>

      <ThemedText style={[styles.sectionHeading, styles.sectionHeadingSpacing]} accessibilityRole="header">
        Support
      </ThemedText>
      <ThemedView style={styles.helpLink}>
        <Link href="/help-modal" asChild>
          <Pressable accessibilityRole="button" accessibilityLabel="Open Help">
            <ThemedText type="link">Help</ThemedText>
          </Pressable>
        </Link>
      </ThemedView>
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
    alignItems: 'center',
    gap: 8,
  },
  sectionHeading: {
    fontSize: 13,
    fontWeight: '600',
    letterSpacing: 0.3,
    textTransform: 'uppercase',
    opacity: 0.65,
  },
  sectionHeadingFirst: {
    marginTop: 24,
  },
  sectionHeadingSpacing: {
    marginTop: 28,
  },
  portionsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    marginTop: 8,
    paddingVertical: 8,
  },
  portionsLabelRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    minWidth: 0,
  },
  infoIconWrap: {
    justifyContent: 'center',
  },
  infoBackdrop: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.35)',
  },
  infoBubble: {
    position: 'absolute',
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 12,
    boxShadow: [
      {
        offsetX: 0,
        offsetY: 2,
        blurRadius: 8,
        color: 'rgba(0, 0, 0, 0.2)',
      },
    ],
    elevation: 6,
  },
  infoBubbleText: {
    fontSize: 15,
    lineHeight: 21,
  },
  portionsInput: {
    width: 72,
    fontSize: 17,
    paddingVertical: 4,
    textAlign: 'right',
  },
  mistralKeyBlock: {
    marginTop: 8,
    gap: 8,
  },
  mistralKeyInput: {
    fontSize: 15,
    paddingVertical: 8,
  },
  clearKeyRow: {
    alignSelf: 'flex-start',
    paddingVertical: 4,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: 8,
    paddingVertical: 8,
  },
  label: {
    fontSize: 17,
  },
  helpLink: {
    marginTop: 4,
    paddingVertical: 8,
  },
  dataActions: {
    marginTop: 4,
    gap: 4,
  },
  dataRow: {
    paddingVertical: 8,
  },
  importQualifier: {
    fontSize: 14,
    fontWeight: '600',
  },
  dataRowPressed: {
    opacity: 0.65,
  },
  dataLinkDisabled: {
    opacity: 0.45,
  },
});
