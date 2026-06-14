# DeepSeek OmegaT Plugin

This project provides an OmegaT machine translation connector for the DeepSeek API.

## What it does

- Registers a DeepSeek machine translation engine inside OmegaT.
- Sends translation requests to the OpenAI-compatible DeepSeek chat completions API.
- Exposes configuration for the API key and model selection in OmegaT preferences.

## Build

```bash
./gradlew build
```

## Install into OmegaT

Copy the generated plugin jar into OmegaT's plugin directory, then restart OmegaT.

## Configuration

- API key: stored in OmegaT credentials.
- Model: defaults to `deepseek-v4-flash`.
- Base URL: defaults to `https://api.deepseek.com` and can be overridden with the `deepseek.api.url` system property.
