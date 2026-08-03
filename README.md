# DeepSeek OmegaT Plugin ![version](https://img.shields.io/badge/version-1.5.3-blue)

This plugin adds DeepSeek as a machine translation provider in OmegaT.

## Features

- Registers a DeepSeek translation engine inside OmegaT.
- Sends requests to the OpenAI-compatible DeepSeek chat completions API.
- Configurable model selection, temperature, and dynamic temperature.
- **Glossary support** — automatically reads OmegaT project glossaries and passes matching entries (with comments) to the AI as translation hints.
- **Context segments** — optionally sends surrounding segments (above/below) to the AI for better continuity and tone consistency across sentences.
- **Auto-insert** — when active, automatically fills the target segment with the machine translation result, eliminating the need to press Ctrl+M for every segment.
- **Auto-confirm** — when active, also commits the translation and advances to the next segment (use with caution).
- **Auto-glossary** — the AI suggests key terminology pairs alongside each translation, including optional usage comments. Entries saved to `deepseek_auto_glossary.txt`.
- **Self-review agent** — a second AI pass reviews each translation for tag preservation, glossary consistency, accuracy, and fluency — correcting errors automatically.
- **Hotkey toggle** — press **Ctrl+Shift+M** anytime to turn auto-mode on/off. Settings define what auto-mode does; the hotkey just switches it.
- **⚡ AUTO indicator** — persistent status bar indicator shows when auto-mode is active.




## Requirements

- OmegaT 6.0 or newer.
- A DeepSeek API key.

## Build

From the project root, run:

```bash
./gradlew build
```

On Windows, use:

```bat
gradlew.bat build
```

The plugin JAR is written to `build/libs/`.

## Install into OmegaT

1. Copy the generated JAR from `build/libs/` into OmegaT's plugin directory.
2. Restart OmegaT.

## Configuration

Open OmegaT's machine translation settings and configure the DeepSeek engine.

| Setting | Default | Description |
|---|---|---|
| API key | *(none)* | Your DeepSeek API key, stored in OmegaT credentials |
| Model | `deepseek-v4-flash` | `deepseek-v4-flash` (faster, cheaper) or `deepseek-v4-pro` (slower, more refined) |
| Temperature | `0.3` | Slider 0.0–2.0 in 0.1 steps. Fades (greys out) when Dynamic Temperature is on — stays visible so you can still see the base value. |
| Dynamic Temperature | Off | When enabled, lets the API auto-adjust temperature — the slider is ignored |
| Glossary | None | **None** — glossary disabled. **Reference** — glossary entries are followed by default; the AI may override an entry only when using it literally would cause a factual, grammatical, or stylistic error (e.g. `白金色` stays `platinum color` even with `金色 → gold color` in the glossary) — never for preference or variety. **Strict** — glossary entries must be used exactly. |
| Context segments | 0 | Number of surrounding segments (above and below) to include as context. 0 = disabled, up to 3. Helps AI maintain narrative continuity and tone. |
| Context char limit | 400 | Max characters per context segment before truncation. Options: 200, 400, 600, 800, 1000, or No limit. Adjust based on your segment size. |

You can also override settings with system properties:

- `deepseek.api.key`
- `deepseek.api.model`
- `deepseek.api.url`

## Glossary Files

When glossary mode is set to **Reference** or **Strict**, the plugin reads standard OmegaT glossary files (`.txt`, `.csv`, `.tab`, `.utf8`) from your project's `glossary` folder. Each line should be tab-separated:

```
source term → target term → comment (optional)
```

Only entries whose source term appears in the current segment are included in the prompt (up to 20, sorted by specificity).

## Context Segments

When set to a value greater than 0, the plugin includes up to N segments above and below the current segment as context in the system prompt. This helps the AI:

- Maintain consistent tone and style across sentences
- Understand narrative flow (especially for novel/creative translation)
- Produce more natural transitions between segments

**Segments above** include both the source text *and* the user's actual stored translation from OmegaT (shown as `SRC → TRG`). This means if you manually edit a translation, the AI sees your corrected version — not its own raw output. Falls back to the plugin's own cached output if no stored translation exists yet.

Context segments are truncated to the configured character limit (200–1000, or no limit). Adjust based on your typical segment size — higher values for paragraph-level segmentation, lower for sentence-level. Default is 400 characters.

## Notes

- The plugin sends only the translated text back to OmegaT.
- The translation prompt asks the API to preserve tags, placeholders, and line breaks.
- In **Reference** glossary mode, glossary entries are followed by default — the AI may only deviate when literal use would cause a factual, grammatical, or stylistic error (e.g., a compound word containing a glossary term must not be split), never for preference or variety.
- Context segments are looked up from the project's ordered entry list using sequential position tracking for efficiency.
- When no OmegaT project is open, glossary and context features are silently skipped with no errors.

## Known Issues

- **Context segments + ellipsis segments**: When **Context segments** is set greater than 0 and the current source segment consists only of punctuation/ellipsis (e.g., `......`), there is a small chance the API will return translations for the *next* few segments instead of the current one. This occurs because the model misidentifies the ellipsis as a scene break or continuation marker when context is provided.

  ![Ellipsis segment misidentification example](docs/images/known-issue-context-segment.png)

  **Workaround**: temporarily set Context segments to 0 when translating isolated punctuation segments, or manually correct the output after translation.

## Changelog

### 1.5.3
- **Fixed: Editor freeze** — `translationCache` (LinkedHashMap with access-order) was not thread-safe. Concurrent `get()`/`put()` from multiple OmegaT worker threads corrupted the internal linked list, causing infinite loops. Wrapped with `Collections.synchronizedMap()`.
- **Changed: Hotkey debouncing** — Ctrl+Shift+M now ignores OS auto-repeat events and enforces a 400ms minimum between toggles, preventing EDT flooding when keys are held down.
- **Removed: Auto-stop on manual click** — the entry-listener state machine (`expectingAutoActivation`/`lastAutoEntryNum`) was removed because it caused EDT re-entrancy issues. Use Ctrl+Shift+M to stop auto-mode instead.

### 1.5.2
- **Fixed: Cached translation insertion** — when auto-mode is toggled ON via Ctrl+Shift+M, the already-generated MT result is inserted directly from cache (no redundant API call).
- **Fixed: Auto-confirm throttle** — advances are now spaced at least 600ms apart via a `javax.swing.Timer`, preventing EDT flooding during rapid auto-translation.
- **Changed: Simplified architecture** — removed the complex entry-listener and state-machine that caused EDT re-entrancy issues on some OmegaT versions.

### 1.5.1
- **Fixed:** `commitAndLeave()` → `commitAndDeactivate()` + `nextUntranslatedEntry()` for correct auto-confirm advancement.
- **Fixed:** Removed double `commitAndDeactivate()` that caused auto-mode to cancel after advancing.
- **Fixed:** Glossary file encoding — switched from `FileWriter` (platform default) to `OutputStreamWriter` with UTF-8 to prevent `????` corruption.


### 1.5.0
- **New: Auto-glossary** — AI suggests terminology pairs with optional `;; comment` usage notes. Saved to `deepseek_auto_glossary.txt` in OmegaT tab-separated format.
- **New: Self-review agent** — second API pass checks tag preservation, glossary consistency, accuracy, and fluency. Lower temperature (0.2) for precision.
- **New: ⚡ AUTO status indicator** — persistent length-label indicator with timer refresh when auto-mode is active.
- **New: Auto-stop on manual navigation** — clicking a different segment or switching files disengages auto-mode. Uses entry-number matching (no time window, works with slow API calls).
- **New: Glossary deduplication** — in-memory + file-based dedup prevents duplicate entries across sessions.
- **Changed: Master toggle** — `Ctrl+Shift+M` toggles `PROPERTY_AUTO_ACTIVE` instead of changing individual settings. Checkboxes define behavior; hotkey controls activation.


### 1.4.1
- Initial release with DeepSeek API integration, model selection, temperature control, glossary modes, and context segments.

### v1.4.1
- Configurable context character limit (200/400/600/800/1000/No limit) instead of hardcoded 200
- Temperature slider fades (greys out) instead of hiding with Dynamic Temperature

### v1.4.0
- **Context segments** — send surrounding segments (above/below) to the AI for narrative continuity
- **Stored translation awareness** — context above uses OmegaT's actual stored translations (user edits respected), not just raw MT output
- Temperature slider now **fades** (greys out) when Dynamic Temperature is enabled instead of disappearing
- Sequential position tracking for efficient context lookups during batch translation

### v1.3.0
- **Glossary mode selector** — None / Reference / Strict
- Glossary comments passed to AI as context
- Smart glossary matching (only current-segment terms, sorted by specificity)

### v1.2.1
- Dynamic temperature toggle and scaling

### v1.2.0
- Temperature slider in settings (default 0.3)

### v1.1.0
- Model dropdown selector (DeepSeek V4 Pro / Flash)

### v1.0.0
- Initial release
