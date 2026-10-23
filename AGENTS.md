# AGENTS.md

## Working rules

- **Keep the architecture section of this file in sync.** If you change the
  high-level architecture (package/module layout, data flow, navigation
  structure, dependency injection, persistence), update the "High-level
  architecture" section below in the same commit.
- **Aim for high test coverage broadly.** Add or extend tests for new and
  changed behavior across all layers: pure logic (import parsing, quantity
  math, shopping-line merging), repositories, backup codec, and ViewModels.
  Tests live in `app/src/test/java/` (JUnit 4 + Robolectric + Turbine +
  coroutines-test); fixtures go in `app/src/test/resources/`. Run
  `./gradlew :app:testDebugUnitTest` before finishing a change.
- Build with `./gradlew assembleDebug`; release with `./gradlew assembleRelease`.
  Requires JDK 21 and Android SDK platform 37.
- Match existing style: 4-space indent, trailing commas, explicit types only
  where the codebase already uses them. Keep modules small and focused; prefer
  pure, testable functions over Android-coupled code.

## High-level architecture

Weeklies is a single-module (`:app`) native Android app: Kotlin, Jetpack
Compose (Material 3), Room, DataStore, Navigation Compose. Package root:
`io.github.conorryan.weeklies`. Fully offline; the only network call is the
optional Mistral AI recipe import.

### Data flow

```
AppDatabase (Room) / settings stores (DataStore)
        │ Flows
        ▼
Repositories — single door from UI to persistence
        │ Flows
        ▼
ViewModels ──state──► Compose screens ──feedback──► ToastBus (snackbar in AppRoot)
        │
        └── suspend mutations ──► Repositories ──► AppDatabase / DataStore

BackupManager   export: repos + settings → one JSON file; import: JSON → replaces everything
FolderBackups   BackupManager → timestamped JSON in the user-picked SAF folder (newest 3 kept)
AutoBackup      repos' flows → debounced FolderBackups write after data changes while enabled
RecipeImporter  URL → PageFetcher/Scraper → JSON-LD parse → optional Mistral → recipe
```

- **Entry / DI** — `WeekliesApplication` builds a manual dependency graph
  (`AppGraph` in `WeekliesApplication.kt`): one Room `AppDatabase`, the three
  settings stores (`AppSettingsStore`, `BackupFolderStore`, `SecretsStore`,
  each backed by the process-wide DataStores in `SettingsDataStores`),
  three repositories, `BackupManager`, `FolderBackups`, and lazy
  `RecipeImporter`/`RecipeCleaner`. No DI framework; screens receive `graph`.
- **Data layer** (`data/`) — the only code that touches persistence:
  - `db/` — Room entities, DAOs, `AppDatabase` (recipes, weekly plan, shopping
    list). Schema v2 added `recipes.instructions` (one text blob); migrations
    live in `AppDatabase` and are covered by a Robolectric test.
  - `model/` — pure domain models (`Recipe`, `ShoppingItem`, `Weekday`, `AppSettings`).
  - `repo/` — `RecipeRepository`, `PlanRepository`, `ShoppingRepository`: the single
    door from UI to the database. `Flow`s for observation, suspend functions for
    mutations. `ShoppingRepository.addRecipeIngredients` owns the whole
    recipe-to-shopping composition (strip notes, scale by portions, merge
    duplicates) in one transaction.
  - `settings/` — DataStore-backed stores, one per concern: `AppSettingsStore`
    (color scheme, portions per day: the part that travels inside backup
    JSON), `BackupFolderStore` (device-local folder-backup bookkeeping:
    folder URI/name, auto-backup toggle, last-backup timestamp, recorded
    document IDs), and `SecretsStore` (Mistral API key; never included in
    backups). `SettingsDataStores` owns the process-wide DataStore instances;
    the stores accept their DataStore through the constructor so tests can
    substitute isolated file-backed ones.
  - `backup/` — `BackupCodec` (JSON encode/decode, compatible with the previous
    React Native format) and `BackupManager` (export all data to one JSON file;
    import replaces everything, no merge); `FolderBackups` (off-device backups:
    writes timestamped JSON into a user-picked SAF folder such as Google Drive
    or Nextcloud, prunes to the newest 3 — only files this app created, tracked
    by recorded document IDs, are ever deleted — and holds a persisted tree
    permission). `writeBackupNow()` owns the whole concept: resolve the
    configured folder, write, prune, record the timestamp.
    `AutoBackup` (debounced write after data changes while enabled,
    started app-wide from `WeekliesApplication` via `graph.appScope`).
  - `import/` — recipe import subsystem: `PageFetcher`/`Scraper` (OkHttp + Jsoup),
    JSON-LD parsing (`ScraperParse`, `ScraperToRecipe`, `Preprocess`), optional
    Mistral completion (`MistralClient`, which also holds the Mistral response
    parsing, with `ImportConstants`; recipe-servings scaling lives in
    `RecipeServings.kt`; `RecipeImporter` is the pipeline's single external
    interface), and the AI cleanup of ingredients and instructions
    (`RecipeCleaner` in `RecipeCleanup.kt`). `MistralChat` is the one place
    that knows the Mistral wire protocol (request body, strict JSON schema,
    HTTP error map, content extraction, retry-once); both `MistralClient` and
    `RecipeCleaner` sit on it.
  - `quantity/` — ingredient-quantity math (`QuantityMath.kt`: numeric token
    matching, parsing, formatting, `scaleIngredientLine`,
    `ingredientPartsToLine`): the neutral home both `data.import` and
    `data.shopping` sit above.
  - `shopping/` — `combineShoppingLine`: pure logic merging duplicate lines
    ("100g pasta" + "100g pasta" -> "200g pasta").
- **UI layer** (`ui/`) — Compose screens + ViewModels, one package per feature:
  `plan/`, `recipes/`, `shopping/`, `settings/`, `import/`, `support/`
  (help/privacy), plus `theme/`, `Common.kt`, `ToastBus.kt`. `AppRoot.kt` holds
  `WeekliesApp`, the `Routes` object, and `PersistentTabHost`, which keeps all
  four tab screens composed so state stays warm across switches (only the active
  tab is measured/drawn) above a `NavHost` with transitions disabled. Tabs:
  Plan, Recipes, Shopping List, Settings. Other destinations (recipe
  new/detail/edit, import, help, privacy) are pushed onto the NavHost.
- **State flow** — ViewModels collect repository `Flow`s
  (`collectAsStateWithLifecycle`) and call suspend mutations; user feedback
  goes through `ToastBus` to a snackbar in `AppRoot`. Every feature has a
  ViewModel, including recipes: `RecipeFormViewModel` (draft, validation, AI
  cleanup, save; draft persisted through `SavedStateHandle`) and
  `RecipesViewModel` (delete, rescale).
- **Tests** — `app/src/test/java/` covers the data layer (backup, import,
  quantity, shopping, settings stores), the repositories (`ShoppingRepository`),
  and ViewModels (`RecipeFormViewModel`); fixtures go in
  `app/src/test/resources/`.

### Notable invariants

- Backup JSON must stay compatible with the previous React Native format
  (`BackupCodec`); backup import replaces everything — no merge. Recipes may
  carry an optional `instructions` string; it is omitted when empty so
  RN-era backups and recipes stay byte-identical.
- The SAF backup folder URI/name, auto-backup toggle, and last-backup timestamp
  are device-local settings in `BackupFolderStore` outside `AppSettings`: they
  never appear in backup JSON and survive backup imports.
- Folder-backup pruning deletes only files this app created (document IDs
  recorded in `BackupFolderStore`) whose names match the exact pattern
  `weeklies-backup-YYYY-MM-DD-HHMMSS.json`; files from other programs in the
  folder are never touched.
- The Mistral API key (`SecretsStore`) is never included in backups.
- Recipe import normalizes ingredient amounts to one portion; the shopping list
  scales lines by **portions per day** when adding today's recipe.
- Shopping-line merging only combines a new line with an existing unchecked
  line whose remaining text matches ("100g pasta" + "100g pasta" -> "200g pasta").
  Adding a recipe's ingredients to the shopping list is one repository
  operation (`ShoppingRepository.addRecipeIngredients`) in one transaction.
- Preparation notes in parentheses are stripped (`stripIngredientNotes`) when a
  recipe's ingredients are added to the shopping list, so "250 g mushrooms
  (trimmed)" is bought as "250 g mushrooms" and merges with it.
- UI never touches DAOs directly — repositories are the only door.
- `PersistentTabHost` keeps all four tab screens composed (state stays warm);
  only the active tab is measured/drawn. A tab route exists in two places —
  `TABS` and an empty composable on the NavHost — new tabs must be wired in both.
- Settings are collected with `collectAsStateWithLifecycle(defaults)` so the
  first frame renders without waiting on DataStore.
