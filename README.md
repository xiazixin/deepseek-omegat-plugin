# DeepSeek OmegaT Plugin

This plugin adds DeepSeek as a machine translation provider in OmegaT.

## Features

- Registers a DeepSeek translation engine inside OmegaT.
- Sends requests to the OpenAI-compatible DeepSeek chat completions API.
- Supports per-user API key storage and configurable model selection in OmegaT preferences.

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

- API key: stored in OmegaT credentials.
- Model: defaults to `deepseek-v4-flash`.
- Base URL: defaults to `https://api.deepseek.com`.

You can also override settings with system properties:

- `deepseek.api.key`
- `deepseek.api.model`
- `deepseek.api.url`

## Notes

- The plugin sends only the translated text back to OmegaT.
- The translation prompt asks the API to preserve tags, placeholders, and line breaks.
