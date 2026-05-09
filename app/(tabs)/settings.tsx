import { Link } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Alert, Dimensions, Modal, Pressable, StyleSheet, Switch, View } from 'react-native';

import ParallaxScrollView from '@/components/parallax-scroll-view';
import { ThemedText } from '@/components/themed-text';
import { ThemedTextInput } from '@/components/themed-text-input';
import { ThemedView } from '@/components/themed-view';
import { IconSymbol } from '@/components/ui/icon-symbol';
import { Colors, Fonts, SettingsScreenHeader } from '@/constants/theme';
import {
  isValidPortionsPerDay,
  usePortionsPerDay,
  useSettings,
  useThemePreference,
} from '@/contexts/settings';
import { useAppColorScheme } from '@/hooks/use-app-color-scheme';
import { exportBackupToFilesystem, pickAndImportBackup } from '@/utils/backup';

const PORTIONS_INFO_TEXT = 'Used to scale recipe amounts on your shopping list. Will only work if numeric quantities are included in ingredients.';

export default function SettingsScreen() {
  const colorScheme = useAppColorScheme();
  const { setColorScheme } = useThemePreference();
  const { portionsPerDay, setPortionsPerDay } = usePortionsPerDay();
  const { settings, replaceSettings } = useSettings();
  const [backupBusy, setBackupBusy] = useState(false);
  const [portionsText, setPortionsText] = useState(() => String(portionsPerDay));
  const [portionsInfoOpen, setPortionsInfoOpen] = useState(false);
  const [portionsInfoAnchor, setPortionsInfoAnchor] = useState<{
    x: number;
    y: number;
    width: number;
    height: number;
  } | null>(null);
  const portionsInfoRef = useRef<View>(null);
  const tint = Colors[colorScheme].tint;

  useEffect(() => {
    setPortionsText(String(portionsPerDay));
  }, [portionsPerDay]);

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
        Alert.alert('Export failed', e instanceof Error ? e.message : 'Unknown error');
      })
      .finally(() => setBackupBusy(false));
  };

  const handleImportPress = () => {
    Alert.alert(
      'Replace all data?',
      'Importing will replace your recipes, weekly plan, shopping list, and settings. This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Replace',
          style: 'destructive',
          onPress: () => {
            setBackupBusy(true);
            void pickAndImportBackup(replaceSettings)
              .catch((e: unknown) => {
                Alert.alert('Import failed', e instanceof Error ? e.message : 'Unknown error');
              })
              .finally(() => setBackupBusy(false));
          },
        },
      ]
    );
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
          onPress={handleImportPress}
          disabled={backupBusy}
          accessibilityRole="button"
          accessibilityLabel="Import data from JSON file"
          style={({ pressed }) => [styles.dataRow, pressed && styles.dataRowPressed]}>
          <ThemedText type="link" style={backupBusy ? styles.dataLinkDisabled : undefined}>
            Import…
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
    ...StyleSheet.absoluteFillObject,
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
  dataRowPressed: {
    opacity: 0.65,
  },
  dataLinkDisabled: {
    opacity: 0.45,
  },
});
