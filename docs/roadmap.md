# PhraseVoice Roadmap

## Phase 1 MVP

- Create the Android project and app module.
- Build the Compose + Material 3 shell.
- Implement the `TtsProvider` abstraction.
- Implement `AndroidSystemTtsProvider` with `speak`, `stop`, voice listing, and WAV export.
- Implement phrase CRUD, favorites, search, quick speak, and history.
- Store ordinary app data in DataStore as JSON.
- Verify builds through GitHub Actions CI.

## Phase 2 Cloud Providers

- Implement `OpenAiTtsProvider` with `gpt-4o-mini-tts` as the default model.
- Implement `GeminiTtsProvider` with isolated response parsing.
- Implement `CustomHttpTtsProvider` for MiMo, OpenAI-compatible services, and user-managed TTS endpoints.
- Add secure provider settings for API keys, base URLs, headers, models, voices, and request templates.

## Phase 3 Polish

- Add audio cache controls and sharing.
- Add import/export for phrases as JSON.
- Expand unit and UI state tests.
- Add screenshots and release docs.
