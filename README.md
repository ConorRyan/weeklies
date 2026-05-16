# Weeklies

Weeklies is a weekly meal planner for people who do not want to ask themselves what to eat every day.

Preview it at [https://weeklies.expo.app](https://weeklies.expo.app).

Add the meals you already know how to cook, store the ingredients for each one, and choose a recipe for every day of the week. The recipe storage is intentionally ingredient-focused; it is not trying to be a cooking instruction book.

You can run the same meals over multiple weeks, change a few days when you feel like it, or rebuild the week from scratch. Weeklies is fully local and completely offline.

## Features

- Save known recipes with their ingredients.
- Edit a recipe from its detail screen (**Edit recipe**): the form matches **New recipe**, with existing data filled in; saving updates that recipe in place (same id everywhere it appears in your plan).
- Plan a recipe for each day of the week; today’s row is tinted and labeled in bold so you can see where you are in the week at a glance.
- Reuse a weekly plan across multiple weeks.
- Change individual meals whenever your plans change.
- Shopping list: add lines manually, tap the row (not only the checkbox) to check items off, clear every checked line with **Remove checked**, and append **today’s planned recipe** with quantities scaled by **portions per day** from Settings (numbers in each ingredient line are multiplied).
- Keep everything fully local and completely offline.
- **Backup (Settings → Data):** export all app data to a single JSON file (`backup-YYYY-MM-DD.json`)—recipes, weekly plan, shopping list, and settings. On native builds you choose where to save it (the picker opens from your app documents folder); on web the browser downloads the file. **Import** loads a backup file and **replaces** everything currently stored in the app (no merge). Use a development or release build that includes the Expo document-picker and file-system native modules; backup UI is not available in environments where those modules are missing.

Settings includes dark mode and **portions per day**, which drives scaling when adding today’s recipe to the shopping list.

## Development

Install dependencies:

```bash
npm install
```

Start Expo:

```bash
npm run start
```

Create Android builds with EAS:

```bash
npm run build:android
npm run build:android:preview
```

## Web (EAS Hosting)

The web build is the same Expo static export as before; hosting is [EAS Hosting](https://docs.expo.dev/eas/hosting/get-started/).

```bash
npm run build:web        # writes to dist/ — local preview: npx expo serve
npm run deploy:web       # preview deployment
npm run deploy:web:prod  # production deployment
```

**Backup on web:** EAS Hosting does not remove the need for web-specific code. In the browser there is no native folder picker—export still uses a download (`Platform.OS === 'web'` in `utils/backup.ts`), and import uses the file from the document picker. iOS/Android keep `Directory.pickDirectoryAsync` / `ExpoFile` paths. That split is about the runtime (browser vs native), not GitHub Pages vs EAS.

## Contributing

Contributions are welcome. Open an issue or pull request if you have a bug fix, improvement, or idea that fits the direction of the app.

## License

Weeklies is released under the [MIT License](./LICENSE).
