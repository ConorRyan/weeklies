# Glossary

Domain vocabulary for Weeklies. Code names and UI copy should use these
terms; when a new concept is named, add it here.

- **Recipe** — a dish: name, ordered ingredient lines, and an optional
  instructions text blob. Stored amounts are always normalized to a single
  portion (see **portions**).
- **Ingredient line** — one row of a recipe's ingredient list, a free-text
  amount plus name, e.g. "250 g mushrooms (trimmed)".
- **Preparation note** — a parenthesized remark at the end of an ingredient
  line ("(trimmed)", "(finely chopped)"). Stripped before shopping-list
  merging so the same ingredient combines.
- **Weekly plan** — the assignment of one recipe per weekday ("the plan");
  "today's recipe" is the plan entry for the weekday containing now.
- **Portions** — two related meanings kept distinct:
  - **portions per day** — the settings value the shopping list scales by
    when adding a recipe's ingredients.
  - **servings** — how many portions a recipe's listed amounts make; import
    uses it only to scale amounts to a single portion.
- **Shopping line** — one row of the shopping list. Adding a line merges it
  into an existing unchecked line whose remaining text matches, summing the
  first number.
- **Recipe draft** — the recipe being written in the form (name, ingredient
  rows, instructions) before it is saved; survives process death.
- **Import** — turning a recipe URL into a recipe draft, by scraping the
  page's JSON-LD (no key) or by asking Mistral with the user's API key.
- **Cleanup** — the AI rewrite of a saved draft's ingredient lines (one unit
  each, metric where convertible) or instructions (a concise checklist),
  always shown as a before/after preview before applying.
- **Mistral chat** — one strict-schema chat request to Mistral's completions
  endpoint; the shared wire protocol behind both import and cleanup.
- **Quantity parsing** — reading, scaling, and formatting the numeric token
  of an ingredient or shopping line (fractions, mixed numbers, decimals,
  comma-grouped integers). Shared by import scaling and shopping merging.
- **Backup** — one JSON export of all data (plan, recipes, shopping list,
  settings), byte-compatible with the previous React Native format. Import
  replaces everything; no merge.
- **Folder backup** — a timestamped backup written into the user-picked SAF
  folder (off-device sync handled by the provider); the newest three are
  kept. Pruning deletes only files this app created. The folder choice and
  its bookkeeping are device-local and never travel inside backup JSON.
