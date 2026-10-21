# Weeklies

Weeklies is a weekly meal planner for people who do not want to ask themselves what to eat every day.

Add the meals you already know how to cook, store the ingredients for each one, and choose a recipe for every day of the week. The recipe storage is intentionally ingredient-focused; it is not trying to be a cooking instruction book.

You can run the same meals over multiple weeks, change a few days when you feel like it, or rebuild the week from scratch. Weeklies is fully local and completely offline.

## Features

- Save known recipes with their ingredients.
- Edit a recipe from its detail screen (**Edit recipe**): the form matches **New recipe**, with existing data filled in; saving updates that recipe in place (same id everywhere it appears in your plan).
- Plan a recipe for each day of the week; today's row is tinted and labeled in bold so you can see where you are in the week at a glance.
- Reuse a weekly plan across multiple weeks.
- Change individual meals whenever your plans change.
- Shopping list: add lines manually, check items off, clear every checked line with **Remove checked**, and append **today's planned recipe** with quantities scaled by **portions per day** from Settings (numbers in each ingredient line are multiplied).
- Keep recipes, your plan, and the shopping list fully local on device.
- **Import with AI (Recipes → Import with AI):** optional online feature using your own [Mistral](https://mistral.ai) API key (Settings → Recipe import). Paste a recipe URL; ingredient amounts are normalized to **one portion** using the recipe's stated servings. The key is stored only on this device and is **not** included in backup export/import. Review and edit before saving.
- **Import (no key needed):** reads recipe data from JSON-LD on the page, entirely on this device.
- **Backup (Settings → Data):** export all app data to a single JSON file (`backup-YYYY-MM-DD.json`) — recipes, weekly plan, shopping list, and settings — via the system file picker. **Import** loads a backup file and **replaces** everything currently stored in the app (no merge).

Settings includes dark mode, **portions per day** (shopping-list scaling), and an optional **Mistral API key** for AI recipe import.

## Development

This is a native Android app: Kotlin 2.4, Jetpack Compose (Material 3), Room, DataStore, Navigation Compose, OkHttp, Jsoup, and kotlinx.serialization. minSdk 26, targetSdk 36, compileSdk 37.

Requirements:

- JDK 21
- Android SDK (platform 37, build-tools, platform-tools)

Build and run:

```bash
./gradlew assembleDebug        # debug APK
./gradlew :app:testDebugUnitTest  # JVM + Robolectric tests
./gradlew assembleRelease      # release APK
```

Install the debug APK on a connected device/emulator with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

License report for the FOSS audit:

```bash
./gradlew :app:generateLicenseReport
```

## Architecture

- `app/src/main/java/io/github/conorryan/weeklies/data/` — Room database (recipes, plan, shopping list), DataStore-backed settings and secrets, backup codec/manager (JSON format compatible with previous React Native releases), and the recipe import subsystem (JSON-LD scraper, Mistral client, ingredient scaling).
- `app/src/main/java/io/github/conorryan/weeklies/ui/` — Compose screens: four tabs (plan, recipes, shopping list, settings) plus recipe detail/edit/new, import with review, help, and privacy.
- `app/src/test/java/` — unit tests ported from the original test suite, reusing the same HTML/JSON fixtures.

