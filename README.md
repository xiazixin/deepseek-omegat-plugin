# DeepSeek OmegaT Plugin (Dev) ![version](https://img.shields.io/badge/version-1.4.1--dev-orange)

> **Development fork** — this is a development build with experimental features and debug tooling. For the stable release, see the [upstream repository](https://github.com/xiazixin/deepseek-omegat-plugin).

This plugin adds DeepSeek as a machine translation provider in OmegaT.

## Features

- Registers a DeepSeek translation engine inside OmegaT.
- Sends requests to the OpenAI-compatible DeepSeek chat completions API.
- Configurable model selection, temperature, and dynamic temperature.
- **Glossary support** — automatically reads OmegaT project glossaries and passes matching entries (with comments) to the AI as translation hints.
- **Context segments** — optionally sends surrounding segments (above/below) to the AI for better continuity and tone consistency across sentences.
- **Debug mode** — when enabled, logs the full JSON request and response to OmegaT's log for troubleshooting.

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
| Glossary | None | **None** — glossary disabled. **Reference** — glossary entries are sent as hints; the AI uses judgment and won't blindly override compound terms (e.g. `白金色` stays `platinum color` even with `金色 → gold color` in the glossary). **Strict** — glossary entries must be used exactly. |
| Context segments | 0 | Number of surrounding segments (above and below) to include as context. 0 = disabled, up to 3. Helps AI maintain narrative continuity and tone. |
| Context char limit | 400 | Max characters per context segment before truncation. Options: 200, 400, 600, 800, 1000, or No limit. Adjust based on your segment size. |
| Debug mode | Off | When enabled, logs the full JSON request sent to DeepSeek and the full JSON response to OmegaT's log (`[DeepSeek Debug] >>>` / `<<<`). Useful for troubleshooting prompts, glossary injection, and API errors. |

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
- In **Reference** glossary mode, glossary entries are sent as contextual hints — the AI is instructed to use judgment and not blindly apply partial matches (e.g., compound words containing a glossary term won't be incorrectly split).
- Context segments are looked up from the project's ordered entry list using sequential position tracking for efficiency.
- When no OmegaT project is open, glossary and context features are silently skipped with no errors.
- Debug mode logs can be found in OmegaT's log viewer. Look for `[DeepSeek Debug]` prefixed entries.

## Debug Output

When **Debug mode** is enabled, every translation request logs both the outgoing JSON and the incoming response. Here is an annotated example with **Glossary: Reference**, **Context segments: 3**, **Context limit: No limit**, and **Model: deepseek-v4-flash**:

### Request (what is sent to DeepSeek)

```json
{
  "messages": [
    {
      "content": "You are a professional translation engine for OmegaT. Translate from zh-CN to en-US. ...\n\nSurrounding context for reference (DO NOT translate these — only the current segment):\n[Above] 不过和第一次相比，这次伊恩就看不见自己身上的黑气。  →  However, compared to the first time, this time Ian could not see the black mist on himself. /// ...\n[Below] 黑曜石的刀刃边缘，有一层暗红的色泽... /// ...\n\nReference glossary — use judgment...:\n- 伊恩 → Ian  [Name of main character]\n伊恩认识那小刀。",
      "role": "system"
    },
    {
      "content": "伊恩认识那小刀。",
      "role": "user"
    }
  ],
  "model": "deepseek-v4-flash",
  "stream": false
}
```

**Key:**

| Part | Label in JSON | Description |
|---|---|---|
| 🔶 Context above | `[Above] … /// …` | Up to N segments *above* the current one, each shown as `SOURCE  →  TRANSLATION` |
| 🔸 Context below | `[Below] … /// …` | Up to N segments *below* the current one (source text only) |
| 🟢 Glossary | `- source → target [comment]` | Matching glossary entries from the project's glossary folder |
| 🔴 Current segment | `"content":"…","role":"user"` | The actual segment that needs to be translated |

### Response (what DeepSeek returns)

```json
{
  "id": "...",
  "model": "deepseek-v4-flash",
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "Ian recognized the knife.",
        "reasoning_content": "We need to translate the Chinese sentence... '认识' can mean 'recognize' or 'know'. Given context of Ian seeing the knife, 'recognized' fits better..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 346,
    "completion_tokens": 170,
    "total_tokens": 516
  }
}
```

**Key:**

| Part | Field | Description |
|---|---|---|
| 🔵 Reasoning | `reasoning_content` | The AI's internal chain-of-thought — how it arrived at the translation (DeepSeek V4 Pro/Flash reasoning models only) |
| ✅ Final output | `content` | The translated text returned to OmegaT |
| 📊 Token usage | `usage` | Prompt tokens, completion tokens, and total — useful for cost estimation |

## Changelog

### v1.4.1-dev (development fork)
- **Debug mode** — new checkbox in settings to log raw API requests & responses for troubleshooting
- Marked as development build (`-dev` suffix on version, `(Dev)` in plugin name)

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
